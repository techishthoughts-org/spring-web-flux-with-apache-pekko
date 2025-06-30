package com.techishthoughts.stocks.adapter.out.mapper;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FinnhubMapper {

    @Mapping(target = "symbol", source = "symbol.symbol")
    @Mapping(target = "name", source = "profile.name")
    @Mapping(target = "exchange", source = "profile.exchange")
    @Mapping(target = "assetType", source = "symbol.type")
    @Mapping(target = "ipoDate", source = "profile.ipo")
    @Mapping(target = "country", source = "profile.country")
    @Mapping(target = "currency", source = "profile.currency")
    @Mapping(target = "ipo", source = "profile.ipo")
    @Mapping(target = "marketCapitalization", source = "profile.marketCapitalization")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "shareOutstanding", source = "profile.shareOutstanding")
    @Mapping(target = "ticker", source = "profile.ticker")
    @Mapping(target = "weburl", source = "profile.weburl")
    @Mapping(target = "logo", source = "profile.logo")
    @Mapping(target = "finnhubIndustry", source = "profile.finnhubIndustry")
    StockDetails toStockDetails(StockSymbols symbol, CompanyProfile profile);
}
