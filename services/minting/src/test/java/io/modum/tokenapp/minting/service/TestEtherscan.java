package io.modum.tokenapp.minting.service;

import io.modum.tokenapp.minting.MintingApplication;
import io.modum.tokenapp.minting.TokenAppBaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@SpringBootTest(classes = MintingApplication.class)
@RunWith(SpringRunner.class)
public class TestEtherscan extends TokenAppBaseTest {

    @Autowired
    private Etherscan etherscan;


    @Autowired
    private Blockr blockr;

    @Test
    public void testConnect1() {
        String balance = etherscan.getBalance("0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae").toString();
        System.out.println("balance: "+balance);
    }

    @Test
    public void testConnect2() {
        String balance = etherscan.get20Balances("0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae", "0x25d96310cd6694d88b9c6803be09511597c0a630").toString();
        System.out.println("balance: "+balance);
    }

    @Test
    public void testBlockNr() {
        long bl1 = blockr.getCurrentBlockNr();
        long bl2 = etherscan.getCurrentBlockNr();
        System.out.println("ret: "+bl1 +"/"+bl2);
    }
}