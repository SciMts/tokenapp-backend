package io.modum.tokenapp.backend.dao;

import io.modum.tokenapp.backend.model.ExchangeRate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ExchangeRateRepository extends CrudRepository<ExchangeRate, Long> {

    @Query(value = "select rate_eth from exchange_rate order by id desc limit 1", nativeQuery = true)
    long getEthPrice();

    @Query(value = "select rate_Btc from exchange_rate order by id desc limit 1", nativeQuery = true)
    long getBtcPrice();
}
