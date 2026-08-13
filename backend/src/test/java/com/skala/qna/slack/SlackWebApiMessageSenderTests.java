package com.skala.qna.slack;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.Authenticator;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;

class SlackWebApiMessageSenderTests {

	@Test
	void missingTokenFailsWithoutMakingAnHttpCall() {
		StubHttpClient httpClient = new StubHttpClient("{\"ok\":true}");

		SlackSendResult result = new SlackWebApiMessageSender("", httpClient).send("C123", "테스트");

		assertThat(result.sent()).isFalse();
		assertThat(result.error()).contains("SLACK_BOT_TOKEN");
		assertThat(httpClient.request).isNull();
	}

	@Test
	void sendsJsonWithBearerTokenAndReadsSlackResponse() {
		StubHttpClient httpClient = new StubHttpClient("{\"ok\":true,\"ts\":\"123.456\"}");

		SlackSendResult result = new SlackWebApiMessageSender("xoxb-test", httpClient).send("C123", "hello \"Slack\"");

		assertThat(result.sent()).isTrue();
		assertThat(result.timestamp()).isEqualTo("123.456");
		assertThat(httpClient.request.headers().firstValue("Authorization")).contains("Bearer xoxb-test");
		assertThat(httpClient.request.bodyPublisher().orElseThrow().contentLength()).isGreaterThan(0);
	}

	@Test
	void opensConversationBeforeSendingDirectMessage() {
		StubHttpClient httpClient = new StubHttpClient("{\"ok\":true,\"channel\":{\"id\":\"D123\"}}");

		SlackSendResult result = new SlackWebApiMessageSender("xoxb-test", httpClient)
				.sendDirectMessage("U123", "DM 테스트");

		assertThat(result.sent()).isTrue();
		assertThat(result.channelId()).isEqualTo("D123");
	}

	private static final class StubHttpClient extends HttpClient {

		private final HttpResponse<String> response;
		private HttpRequest request;

		private StubHttpClient(String body) {
			this.response = new StubHttpResponse(body);
		}

		@Override
		public Optional<CookieHandler> cookieHandler() {
			return Optional.empty();
		}

		@Override
		public Optional<Duration> connectTimeout() {
			return Optional.empty();
		}

		@Override
		public Redirect followRedirects() {
			return Redirect.NEVER;
		}

		@Override
		public Optional<ProxySelector> proxy() {
			return Optional.empty();
		}

		@Override
		public SSLContext sslContext() {
			try {
				return SSLContext.getDefault();
			} catch (NoSuchAlgorithmException exception) {
				throw new IllegalStateException(exception);
			}
		}

		@Override
		public SSLParameters sslParameters() {
			return new SSLParameters();
		}

		@Override
		public Optional<Authenticator> authenticator() {
			return Optional.empty();
		}

		@Override
		public Version version() {
			return Version.HTTP_1_1;
		}

		@Override
		public Optional<Executor> executor() {
			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
			this.request = request;
			return (HttpResponse<T>) response;
		}

		@Override
		public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler,
				PushPromiseHandler<T> pushPromiseHandler) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class StubHttpResponse implements HttpResponse<String> {

		private final String body;

		private StubHttpResponse(String body) {
			this.body = body;
		}

		@Override
		public int statusCode() {
			return 200;
		}

		@Override
		public HttpRequest request() {
			return null;
		}

		@Override
		public Optional<HttpResponse<String>> previousResponse() {
			return Optional.empty();
		}

		@Override
		public HttpHeaders headers() {
			return HttpHeaders.of(Map.of(), (name, value) -> true);
		}

		@Override
		public String body() {
			return body;
		}

		@Override
		public Optional<javax.net.ssl.SSLSession> sslSession() {
			return Optional.empty();
		}

		@Override
		public URI uri() {
			return URI.create("https://slack.com/api/chat.postMessage");
		}

		@Override
		public HttpClient.Version version() {
			return HttpClient.Version.HTTP_1_1;
		}
	}
}
