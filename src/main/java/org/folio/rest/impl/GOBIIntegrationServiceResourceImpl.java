package org.folio.rest.impl;

import java.io.Reader;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.folio.gobi.GobiPurchaseOrderParser;
import org.folio.gobi.GobiResponseWriter;
import org.folio.gobi.exceptions.GobiPurchaseOrderParserException;
import org.folio.gobi.exceptions.HttpException;
import org.folio.rest.RestVerticle;
import org.folio.rest.gobi.model.GobiResponse;
import org.folio.rest.gobi.model.ResponseError;
import org.folio.rest.jaxrs.resource.GOBIIntegrationServiceResource;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class GOBIIntegrationServiceResourceImpl implements GOBIIntegrationServiceResource {

  private static final Logger logger = LoggerFactory.getLogger(GOBIIntegrationServiceResourceImpl.class);

  public static final String OKAPI_HEADER_URL = "X-Okapi-Url";

  @Override
  public void getGobiValidate(Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {
    asyncResultHandler.handle(Future.succeededFuture(GetGobiValidateResponse.withNoContent()));
  }

  @Override
  public void postGobiOrders(Reader entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {

    final GobiResponse response = new GobiResponse();
    final GobiPurchaseOrderParser parser = GobiPurchaseOrderParser.getParser();

    try {
      parser.parse(entity);
    } catch (GobiPurchaseOrderParserException e) {
      final ResponseError re = new ResponseError();
      re.setCode("INVALID_XML");
      re.setMessage(e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)));
      response.setError(re);
      asyncResultHandler.handle(Future.succeededFuture(PostGobiOrdersResponse.withXmlBadRequest(GobiResponseWriter.getWriter().write(response))));
      return;
    }

    final String poLineNumber = new Random().ints(16, 0, 9).mapToObj(Integer::toString).collect(Collectors.joining());
    response.setPoLineNumber(poLineNumber);

    asyncResultHandler.handle(Future.succeededFuture(PostGobiOrdersResponse.withXmlCreated(GobiResponseWriter.getWriter().write(response))));
//    HttpClientInterface httpClient = getHttpClient(okapiHeaders);
//    PostGobiOrdersHelper helper = new PostGobiOrdersHelper(httpClient, asyncResultHandler, okapiHeaders,
//        vertxContext);
//
//    
//    logger.info("Parsing Request...");
//    helper.parse(entity).thenAccept(gobiPO -> {
//      logger.info("Mapping Request...");
//      helper.map(gobiPO).thenAccept(compPO -> {
//        logger.info("Calling mod-orders...");
//        helper.placeOrder(compPO).thenAccept(poLineNumber -> {
//          GobiResponse gobiResponse = new GobiResponse();
//          gobiResponse.setPoLineNumber(poLineNumber);
//
//          javax.ws.rs.core.Response response = PostGobiOrdersResponse
//            .withXmlCreated(GobiResponseWriter.getWriter().write(gobiResponse));
//          AsyncResult<javax.ws.rs.core.Response> result = Future.succeededFuture(response);
//          asyncResultHandler.handle(result);
//
//        }).exceptionally(helper::handleError);
//      }).exceptionally(helper::handleError);
//    }).exceptionally(helper::handleError);
  }

  public static JsonObject verifyAndExtractBody(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      throw new CompletionException(
          new HttpException(response.getCode(), response.getError().getString("errorMessage")));
    }
    return response.getBody();
  }

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_HEADER_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }
}
