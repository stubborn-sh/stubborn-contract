package com.example;

public class Sample {

    // tag::setup[]
    void setup() {
        RestAssuredMockMvc.standaloneSetup(new FraudController());
    }
    // end::setup[]

    // tag::test[]
    @Test
    void should_mark_client_as_fraud() {
        // when
        ResponseOptions response = given()
            .contentType(ContentType.JSON)
            .body("{\"clientId\": 123456, \"loanAmount\": 99999}")
            .when()
            .put("/fraudcheck");
        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getString("fraudCheckStatus")).isEqualTo("FRAUD");
    }
    // end::test[]

    // tag::multiline[]
    // Line one
    // Line two
    // end::multiline[]

}
