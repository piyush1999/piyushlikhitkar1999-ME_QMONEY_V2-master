
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.crio.warmup.stock.quotes.TiingoService;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  RestTemplate restTemplate=new RestTemplate();
  private StockQuotesService stockQuotesService;




  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }
  

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  // public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
  //     PortfolioTrade trade, Double buyPrice, Double sellPrice) {
      
  //       Double absReturn=(sellPrice-buyPrice)/buyPrice;
        
  //       String symbol=trade.getSymbol();
  //       LocalDate purchaseDate=trade.getPurchaseDate();
  //       Double numYears=(double)ChronoUnit.DAYS.between(purchaseDate,endDate)/365;

  //       Double annualizedReturns=Math.pow((1+absReturn),(1/numYears))-1;



  //     return new AnnualizedReturn(symbol, annualizedReturns, absReturn);
  // }



  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) throws JsonProcessingException, StockQuoteServiceException {
    List<AnnualizedReturn> ans = new ArrayList<>();
    for(int i=0;i<portfolioTrades.size();i++) {
      ans.add(getAnnualizedReturn(portfolioTrades.get(i),endDate));
     }
    //  Comparator<AnnualizedReturn> SortByAnnreturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Comparator<AnnualizedReturn> SortByAnnreturn = getComparator();
    Collections.sort(ans, SortByAnnreturn);
     return ans;
  }








  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate) throws JsonProcessingException, StockQuoteServiceException {
    AnnualizedReturn annualizedReturn;
    String symbol = trade.getSymbol();
    LocalDate startDate = trade.getPurchaseDate();
    // try {
      //FETCH DATA
      List<Candle> stockStartToEndDate = getStockQuote(symbol, startDate, endDate);

      //EXTRACT STOCKS FOR START_DATE AND END_DATE
      Candle start = stockStartToEndDate.get(0);
      Candle end = stockStartToEndDate.get(stockStartToEndDate.size()-1);

      Double buyPrice = start.getOpen();
      Double sellPrice = end.getClose();
      //CALCULATE TOTAL RETURNS
      Double totalReturns = (sellPrice - buyPrice) / buyPrice;
      //CALCULATE NO OF YEARS
      Double num_years = (double)ChronoUnit.DAYS.between(startDate, endDate)/365;
      //CALCULATE ANNUAL RETURN BY USING FORMULA
      Double annualizedReturns = Math.pow((1+totalReturns),(1/num_years))-1;
      
      annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturns);

    // } catch (Exception e) {
    //   annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
    // }
    return annualizedReturn;
  }



  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException,StockQuoteServiceException {
    //  if(from.compareTo(to)>=0)
    //  {
    //   throw new RuntimeException();
    //  }

    //  String url=buildUri(symbol, from, to);
    //  TiingoCandle[] stockStartToEndDate=restTemplate.getForObject(url, TiingoCandle[].class);
    //  if(stockStartToEndDate==null)
    //  {return new ArrayList<>();}
    //  else
    //  {
    //   List<Candle> stock=Arrays.asList(stockStartToEndDate);
    //   return stock;
    //  }
   return  stockQuotesService.getStockQuote(symbol, from, to);

  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https:api.tiingo.com/tiingo/daily/"+symbol+"/prices?"
            + "startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+getToken();

            return uriTemplate;
  }
  
  public static String getToken()
  {
    String token="5b1d5b6b44d02abef20411e3c0ba8e568deecd5e";
    return token;
  }
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
        
        List<AnnualizedReturn> annualizedReturns=new ArrayList<>();
        List<Future<AnnualizedReturn>> futureReturnsList =new ArrayList<Future<AnnualizedReturn>>();
        final ExecutorService pool =Executors.newFixedThreadPool(numThreads);

        for(int i=0;i<portfolioTrades.size();i++)
        {
          PortfolioTrade trade=portfolioTrades.get(i);
          Callable<AnnualizedReturn> callableTask=()->{
            return getAnnualizedReturn(trade,endDate);
          } ;
          Future<AnnualizedReturn> futureReturns =pool.submit(callableTask);
          futureReturnsList.add(futureReturns); 
        }

        for(int i=0;i<portfolioTrades.size();i++)
        {
          Future<AnnualizedReturn> futureReturns=futureReturnsList.get(i);
          try{
            AnnualizedReturn returns =futureReturns.get();
            annualizedReturns.add(returns);

          }
          catch(ExecutionException e){
            throw new StockQuoteServiceException("The task did not finish in time",e);

          }
          }
          Comparator<AnnualizedReturn> SortByAnnreturn = getComparator();
          Collections.sort(annualizedReturns, SortByAnnreturn);
        //Collections.sort(annualizedReturns,Collections.reverseOrder());
        // for(int i=0;i<annualizedReturns.size();i++)
        // {
        //   System.out.println(annualizedReturns.get(i).getAnnualizedReturn()+"   andmdkjkslfj "+annualizedReturns.get(i).getSymbol() );
        // }
        return annualizedReturns;
    
  }



  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.



}
