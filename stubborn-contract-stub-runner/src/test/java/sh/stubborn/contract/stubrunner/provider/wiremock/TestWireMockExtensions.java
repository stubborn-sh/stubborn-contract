/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.stubborn.contract.stubrunner.provider.wiremock;

import java.util.List;

import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import sh.stubborn.contract.verifier.dsl.wiremock.DefaultResponseTransformer;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockExtensions;

public class TestWireMockExtensions implements WireMockExtensions {

	@Override
	public List<Extension> extensions() {
		return List.of(new DefaultResponseTransformer(), new CustomExtension());
	}

	static class CustomExtension implements ResponseTransformerV2 {

		@Override
		public String getName() {
			return "foo-transformer";
		}

		@Override
		public Response transform(Response response, ServeEvent serveEvent) {
			return Response.Builder.like(response)
				.but()
				.headers(response.getHeaders().plus(new HttpHeader("X-My-Header", "surprise!")))
				.build();
		}

		@Override
		public boolean applyGlobally() {
			return false;
		}

	}

}
