package io.modum.tokenapp.rates;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.NONE)
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url: jdbc:h2:mem:testdb;mv_store=false;DB_CLOSE_ON_EXIT=FALSE",
        "modum.tokenapp.email.enabled: false",
        "bitcoin.net: unittest",
        "modum.url.etherscan:api.etherscan.io",
        "modum.url.blockr:btc.blockr.io",
        "modum.token.etherscan:" + System.getenv().get("ETHERSCAN_API_TOKEN")

})
// ;mv_store=false needed for correct isolation level:
// http://h2-database.66688.n3.nabble.com/Am-I-bananas-or-does-serializable-isolation-not-work-as-it-should-tp4030767p4030768.html
public abstract class TokenAppBaseTest {
}
