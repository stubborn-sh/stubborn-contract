package contracts

import org.springframework.cloud.contract.spec.Contract

// tag::contract[]
Contract.make {
    request {
        method PUT()
        url '/fraudcheck'
        body([clientId: 1234567890, loanAmount: 99999])
        headers { contentType('application/json') }
    }
    response {
        status OK()
        body([fraudCheckStatus: "FRAUD", rejection.reason: "Amount too high"])
        headers { contentType('application/json') }
    }
}
// end::contract[]
