package io.modum.tokenapp.backend.controller;

import io.modum.tokenapp.backend.controller.exceptions.*;
import io.modum.tokenapp.backend.dao.InvestorRepository;
import io.modum.tokenapp.backend.dao.KeyPairsRepository;
import io.modum.tokenapp.backend.dao.ExchangeRateRepository;
import io.modum.tokenapp.backend.dto.AddressRequest;
import io.modum.tokenapp.backend.dto.AddressResponse;
import io.modum.tokenapp.backend.dto.StatusResponse;
import io.modum.tokenapp.backend.model.Investor;
import io.modum.tokenapp.backend.model.KeyPairs;
import io.modum.tokenapp.backend.service.AddressService;
import io.modum.tokenapp.backend.service.FileQueueService;
import io.modum.tokenapp.backend.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.ws.rs.core.Context;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@EnableWebMvc
public class AddressController {

    private static final Logger LOG = LoggerFactory.getLogger(AddressController.class);

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private KeyPairsRepository keyPairsRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private FileQueueService fileQueueService;

    public AddressController() {

    }

    @RequestMapping(value = "status", method = GET)
    public ResponseEntity<StatusResponse> status(@Context HttpServletRequest httpServletRequest) throws BaseException {
        long ethPrice = exchangeRateRepository.getEthPrice();
        long btcPrice = exchangeRateRepository.getBtcPrice();
        return new ResponseEntity<>(new StatusResponse().setEthPrice(ethPrice).setBtcPrice(btcPrice)
            , HttpStatus.OK);

    }

    @RequestMapping(value = "address", method = POST, consumes = APPLICATION_JSON_UTF8_VALUE,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddressResponse> address(@Valid @RequestBody AddressRequest addressRequest,
                                                   @Valid @Size(max = Constants.UUID_CHAR_MAX_SIZE) @RequestHeader(value="Authorization") String authorizationHeader,
                                                   @Context HttpServletRequest httpServletRequest)
            throws BaseException {
        // Get token
        String emailConfirmationToken = getEmailConfirmationToken(authorizationHeader);

        // Get IP address from request
        String ipAddress = httpServletRequest.getHeader("X-Real-IP");
        if (ipAddress == null)
            ipAddress = httpServletRequest.getRemoteAddr();
        LOG.info("/address called from {} with token {}, address {}, refundBTC {} refundETH {}",
                ipAddress,
                emailConfirmationToken,
                addressRequest.getAddress(),
                addressRequest.getRefundBTC(),
                addressRequest.getRefundETH());

        return setWalletAddress(addressRequest, emailConfirmationToken);
    }

    @Transactional
    public ResponseEntity<AddressResponse> setWalletAddress(AddressRequest addressRequest, String emailConfirmationToken)
            throws ConfirmationTokenNotFoundException, WalletAddressAlreadySetException,
            EthereumWalletAddressEmptyException, BitcoinAddressInvalidException, EthereumAddressInvalidException,
            UnexpectedException {
        // Get the user that belongs to the token
        Optional<Investor> oInvestor = findInvestorOrThrowException(emailConfirmationToken);

        // Return 409 if the WalletAddress is already set
        if (oInvestor.get().getWalletAddress() != null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // Get refund addresses and if set check for validity
        String refundBitcoinAddress = addressRequest.getRefundBTC();
        String refundEthereumAddress = replacePrefixAddress(addressRequest.getRefundETH());
        if (refundBitcoinAddress != null && !addressService.isValidBitcoinAddress(refundBitcoinAddress))
            throw new BitcoinAddressInvalidException();

        if (refundEthereumAddress != null && !addressService.isValidEthereumAddress(refundEthereumAddress))
            throw new EthereumAddressInvalidException();

        String walletAddress = replacePrefixAddress(addressRequest.getAddress());

        // Make sure all addresses are valid and wallet address sis non-empty
        checkWalletAndRefundAddressesOrThrowException(walletAddress, refundEthereumAddress, refundBitcoinAddress);

        // Generating the keys
        long freshKeyId = keyPairsRepository.getFreshKeyID();
        KeyPairs keyPairs = keyPairsRepository.findOne(freshKeyId);

        // Persist the updated investor
        try {
            Investor investor = oInvestor.get();
            investor.setWalletAddress(addPrefixEtherIfNotExist(walletAddress))
                    .setPayInBitcoinPublicKey(keyPairs.getPublicBtc())
                    .setPayInEtherPublicKey(keyPairs.getPublicEth())
                    .setRefundBitcoinAddress(refundBitcoinAddress)
                    .setRefundEtherAddress(addPrefixEtherIfNotExist(refundEthereumAddress));
            investorRepository.save(investor);
            fileQueueService.addSummaryEmail(investor);
        } catch(Exception e) {
            LOG.error("Unexpected exception in AddressController: {} {}", e.getMessage(), e.getCause());
            throw new UnexpectedException();
        }

        // Return DTO
        return new ResponseEntity<>(new AddressResponse()
                .setBtc(addressService.getBitcoinAddressFromPublicKey(keyPairs.getPublicBtc()))
                .setEther(addressService.getEthereumAddressFromPublicKey(keyPairs.getPublicEth())), HttpStatus.OK);
    }

    private String getEmailConfirmationToken(String authorizationHeader) throws AuthorizationHeaderMissingException {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new AuthorizationHeaderMissingException();
        }

        String[] authorizationHeaderSplit = authorizationHeader.split("Bearer ");
        String emailConfirmationToken = authorizationHeaderSplit[authorizationHeaderSplit.length - 1];

        if (emailConfirmationToken == null || emailConfirmationToken.isEmpty()) {
            throw new AuthorizationHeaderMissingException();
        }

        return emailConfirmationToken;
    }

    private String replacePrefixAddress(String address) {
        if (address == null) {
            return null;
        } else {
            return address.replaceAll("^0x", "");
        }
    }

    private String addPrefixEtherIfNotExist(String address) {
        if (address == null) {
            return null;
        } else {
            return address.startsWith("0x") ? address : ("0x" + address);
        }
    }

    private Optional<Investor> findInvestorOrThrowException(String emailConfirmationToken)
            throws ConfirmationTokenNotFoundException {
        Optional<Investor> oInvestor = investorRepository.findOptionalByEmailConfirmationToken(emailConfirmationToken);
        if (!oInvestor.isPresent()) {
            throw new ConfirmationTokenNotFoundException();
        } else {
            return oInvestor;
        }
    }

    private void checkWalletAndRefundAddressesOrThrowException(String walletAddress,
                                                               String refundEthereumAddress,
                                                               String refundBitcoinAddress)
            throws EthereumWalletAddressEmptyException, EthereumAddressInvalidException, BitcoinAddressInvalidException {

        // Check if the wallet is empty
        if (walletAddress.isEmpty()) {
            throw new EthereumWalletAddressEmptyException();
        }

        // Validate wallet address
        if (!addressService.isValidEthereumAddress(walletAddress)) {
            throw new EthereumAddressInvalidException();
        }

        // Check if the Ethereum refund addresses are present and valid
        if (refundEthereumAddress != null
                && !refundEthereumAddress.isEmpty()
                && !addressService.isValidEthereumAddress(refundEthereumAddress)) {
            throw new EthereumAddressInvalidException();
        }
        // Check if the Bitcoin refund addresses are present and valid
        if (refundBitcoinAddress != null
                && !refundBitcoinAddress.isEmpty()
                && !addressService.isValidBitcoinAddress(refundBitcoinAddress)) {
            throw new BitcoinAddressInvalidException();
        }

    }

}
