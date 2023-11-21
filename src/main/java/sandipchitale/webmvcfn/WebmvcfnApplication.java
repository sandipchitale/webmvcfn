package sandipchitale.webmvcfn;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.*;

import java.util.Objects;

import static org.springframework.web.servlet.function.RequestPredicates.methods;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class WebmvcfnApplication {
	public static void main(String[] args) {
		SpringApplication.run(WebmvcfnApplication.class, args);
	}

	private static record Person(String name, int age) {}

	@Bean
	RouterFunction<ServerResponse> routes(@Qualifier("serverResponse") HandlerFunction<ServerResponse> serverResponse) {
		String X_METHOD = "X-METHOD";
		return route()
				.before((ServerRequest request) -> {
					return ServerRequest.from(request).header(X_METHOD, request.method().name().toLowerCase()).build();
				})
				.route(path("/").and(methods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)), serverResponse)
				.after((ServerRequest request, ServerResponse response) -> {
					if (response instanceof EntityResponse<?> entityResponse) {
						return ServerResponse
								.from(entityResponse)
								.header(X_METHOD, request.method().name().toLowerCase())
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.body(entityResponse.entity());
					}
					return response;
				})
				.build();
	}

	@Bean
	@Qualifier("serverResponse")
	@Profile("resttemplate")
	HandlerFunction<ServerResponse> restTemplateServerResponse(RestTemplateBuilder restTemplateBuilder) {
		return (ServerRequest request) -> {
			RestTemplate restTemplate = restTemplateBuilder.rootUri("https://postman-echo.com/").build();
			HttpEntity<Person> personEntity = new HttpEntity<>(new Person("hitchhiker", 42));
			return ServerResponse.ok().body(
					Objects.requireNonNull(
							restTemplate.exchange("/" + request.method().name().toLowerCase(),
									request.method(),
									personEntity,
									String.class).getBody())
			);
		};
	}

	@Bean
	@Qualifier("serverResponse")
	@Profile("!resttemplate")
	HandlerFunction<ServerResponse> restClientServerResponse(RestClient.Builder restClientBuilder) {
		return (ServerRequest request) -> {
			RestClient restClient = restClientBuilder.baseUrl("https://postman-echo.com/").build();
			HttpEntity<Person> personEntity = new HttpEntity<>(new Person("hitchhiker", 42));
			return ServerResponse.ok().body(
					Objects.requireNonNull(
							restClient.method(request.method())
									.uri("/" + request.method().name().toLowerCase())
									.body(personEntity)
									.retrieve()
									.body(String.class))
			);
		};
	}
}
