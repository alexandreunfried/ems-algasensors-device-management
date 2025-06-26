package com.unfried.algasensors.device.management.api.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RestClientFactory {

	private static final String BASE_URL_SENSOR_MONITORING = "http://localhost:8082";

	private final RestClient.Builder builder;

	public RestClient temperatureMonitoringRestClient() {
		return builder.baseUrl(BASE_URL_SENSOR_MONITORING)
				.requestFactory(generateClientHttpRequestFactory())
				.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
					throw new SensorMonitoringClientBadGatewayException();
				})
				.build();
	}

	private ClientHttpRequestFactory generateClientHttpRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

		factory.setReadTimeout(Duration.ofSeconds(5));
		factory.setConnectTimeout(Duration.ofSeconds(3));

		return factory;
	}

}
