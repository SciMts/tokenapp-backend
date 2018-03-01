package io.modum.tokenapp.backend.dto;
import io.modum.tokenapp.backend.utils.Constants;
import javax.validation.constraints.NotNull;

public class StatusResponse {

    @NotNull
    private String ethPrice;

    private String btcPrice;

    public String getEthPrice() {
        return ethPrice;
    }

    public StatusResponse setEthPrice(String ethPrice) {
        this.ethPrice = ethPrice;
        return this;
    }

    public String getBtcPrice() {
        return btcPrice;
    }

    public StatusResponse setBtcPrice(String btcPrice) {
        this.btcPrice = btcPrice;
        return this;
    }
}
