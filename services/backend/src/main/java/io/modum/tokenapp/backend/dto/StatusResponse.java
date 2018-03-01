package io.modum.tokenapp.backend.dto;
import io.modum.tokenapp.backend.utils.Constants;
import javax.validation.constraints.NotNull;

public class StatusResponse {

    @NotNull
    private long ethPrice;

    private long btcPrice;

    public long getEthPrice() {
        return ethPrice;
    }

    public StatusResponse setEthPrice(long ethPrice) {
        this.ethPrice = ethPrice;
        return this;
    }

    public long getBtcPrice() {
        return btcPrice;
    }

    public StatusResponse setBtcPrice(long btcPrice) {
        this.btcPrice = btcPrice;
        return this;
    }
}
