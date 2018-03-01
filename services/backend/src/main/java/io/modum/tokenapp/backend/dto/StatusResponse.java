package io.modum.tokenapp.backend.dto;
import io.modum.tokenapp.backend.utils.Constants;
import javax.validation.constraints.NotNull;

public class StatusResponse {

    @NotNull
    private float ethPrice;

    private float btcPrice;

    public float getEthPrice() {
        return ethPrice;
    }

    public StatusResponse setEthPrice(float ethPrice) {
        this.ethPrice = ethPrice;
        return this;
    }

    public float getBtcPrice() {
        return btcPrice;
    }

    public StatusResponse setBtcPrice(float btcPrice) {
        this.btcPrice = btcPrice;
        return this;
    }
}
