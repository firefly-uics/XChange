package org.knowm.xchange.bitstamp.service.marketdata;

import java.io.IOException;
import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by tom on 2018/8/31. */
public class PublicMarketEUR {

  Logger logger = LoggerFactory.getLogger(PublicMarketEUR.class);
  public static final float ltc_btc_slippage = 0;
  public static final float btc_usd_slippage = 0;
  public static final float ltc_usd_slippage = 0;

  //    LTC/BTC: ltc_btc_fee
  //    BTC/USD: btc_usd_fee
  //    LTC/USD: ltc_usd_fee
  public static final float ltc_btc_fee = 0.001f;
  public static final float btc_usd_fee = 0.001f;
  public static final float ltc_usd_fee = 0.001f;

  public static float arbitrage_fee = 0.0f;

  Exchange bitstamp = ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName());

  MarketDataService marketDataService = bitstamp.getMarketDataService();

  @Test
  public void testArbitrageEUR() {
    PublicMarketEUR pm = new PublicMarketEUR();
    while (true) {

      try {
        Thread.sleep(1000l);
        pm.arbitrage(CurrencyPair.ETH_BTC, CurrencyPair.ETH_EUR, CurrencyPair.BTC_EUR);
      } catch (Exception e) {
        logger.warn("error", e);
      }
    }
  }

  // bid price是买家愿意支付的最高价格,还有很多价格,不过都低于这个最高价位。 ask price是卖家出的最低价格
  //    @Test
  public void arbitrage(CurrencyPair cp3, CurrencyPair cp2, CurrencyPair cp1) throws IOException {
    // P3 = ltc_btc_sell1_price*(1+ltc_btc_slippage)

    float ltc_btc_sell1_price = getSell1(cp3);

    logger.info("ltc_btc_sell1_price:{}", ltc_btc_sell1_price);

    float p3 = ltc_btc_sell1_price * (1 + ltc_btc_slippage);

    logger.info("p3:{}", p3);

    // P1= btc_usd_sell1_price*(1+btc_usd_slippage)

    float btc_usd_sell1_price = getSell1(cp1);
    logger.info("btc_usd_sell1_price:{}", btc_usd_sell1_price);

    float p1 = btc_usd_sell1_price * (1 + btc_usd_slippage);

    logger.info("p1:{}", p1);

    // P2 = ltc_usd_buy1_price*(1-ltc_usd_slippage)

    float ltc_usd_buy1_price = getBuy1(cp2);
    logger.info("ltc_usd_buy1_price:{}", ltc_usd_buy1_price);

    float p2 = ltc_usd_buy1_price * (1 + ltc_usd_slippage);

    logger.info("p2:{}", p2);

    // 在LTC/BTC市场净买入1个LTC，实际上需要买入1/(1-ltc_btc_fee)个LTC，其中的ltc_btc_fee比例部分，是被交易平台收走的手续费。买入1/(1-ltc_btc_fee)个LTC需要花费的BTC数量是
    float sell_ltc_btc_sub = ltc_btc_sell1_price * (1 + ltc_btc_slippage) / (1 - ltc_btc_fee);

    // 在LTC/CNY市场，卖出1个LTC，得到的usd是
    float sell_ltc_usd_add = ltc_usd_buy1_price * (1 - ltc_usd_slippage) * (1 - ltc_usd_fee);

    // 在BTC/usd市场，净买入sell_ltc_btc_sub 个 btc ，实际需要买入
    // ltc_btc_sell_1_price*(1+ltc_btc_slippage)/[(1-ltc_btc_fee)*(1-btc_usd_fee)] 个btc
    float sell_ltc_usd_sub =
        btc_usd_sell1_price
            * (1 + btc_usd_slippage)
            * ltc_btc_sell1_price
            * (1 + ltc_btc_slippage)
            / ((1 - ltc_btc_fee) * (1 - btc_usd_fee));

    if (sell_ltc_usd_add > sell_ltc_usd_sub) {
      logger.warn("arbitrage:{}", sell_ltc_usd_add - sell_ltc_usd_sub);
      arbitrage_fee += (sell_ltc_usd_add - sell_ltc_usd_sub);
    } else {
      logger.info("not arbitrage:{}", sell_ltc_usd_add - sell_ltc_usd_sub);
    }

    logger.info("eth arbitrage_fee:{}", arbitrage_fee);
  }

  public float getSell1(CurrencyPair currencyPair) throws IOException {
    OrderBook orderBook = marketDataService.getOrderBook(currencyPair);
    return orderBook.getAsks().get(0).getLimitPrice().floatValue();
  }

  public float getBuy1(CurrencyPair currencyPair) throws IOException {
    OrderBook orderBook = marketDataService.getOrderBook(currencyPair);
    return orderBook.getBids().get(0).getLimitPrice().floatValue();
  }
}
