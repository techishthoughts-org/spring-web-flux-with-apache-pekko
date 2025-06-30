package com.techishthoughts.stocks.adapter.out.finnhub.dto;

public record CompanyProfile(
String country,
String currency,
String exchange,
String ipo,
Double marketCapitalization,
String name,
String phone,
Double shareOutstanding,
String ticker,
String weburl,
String logo,
String finnhubIndustry
) {
}
