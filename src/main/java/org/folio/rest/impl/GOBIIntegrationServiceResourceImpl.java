package org.folio.rest.impl;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.gobi.GobiResponseWriter;
import org.folio.rest.RestVerticle;
import org.folio.rest.gobi.model.GobiResponse;
import org.folio.rest.jaxrs.resource.GOBIIntegrationServiceResource;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.BinaryOutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class GOBIIntegrationServiceResourceImpl implements GOBIIntegrationServiceResource {

  private static final Logger logger = LoggerFactory.getLogger(GOBIIntegrationServiceResourceImpl.class);
  private static final String GET_DATA = "<test>GET - OK</test>";
  private static final String POST_DATA = "<test>POST - OK</test>";
  
  public static final String OKAPI_HEADER_URL = "X-Okapi-Url";
  

  @Override
  public void getGobiValidate(Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext) throws Exception {
    BinaryOutStream binaryOutStream = new BinaryOutStream();
    binaryOutStream.setData(GET_DATA.getBytes(StandardCharsets.UTF_8));

    asyncResultHandler.handle(Future.succeededFuture(GetGobiValidateResponse.withXmlOK(binaryOutStream)));
  }

  @Override
  public void postGobiOrders(Reader entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler, Context vertxContext)
      throws Exception {

    HttpClientInterface httpClient = getHttpClient(okapiHeaders);
    PostGobiOrdersHelper helper = new PostGobiOrdersHelper(httpClient, asyncResultHandler, okapiHeaders,
        vertxContext);

    logger.info("Parsing Request...");
    helper.parse(entity).thenAccept(gobiPO -> {
      logger.info("Mapping Request...");
      helper.map(gobiPO).thenAccept(compPO -> {
        logger.info("Calling mod-orders...");
        helper.placeOrder(compPO).thenAccept(poLineNumber -> {
          GobiResponse gobiResponse = new GobiResponse();
          gobiResponse.setPoLineNumber(poLineNumber);

          javax.ws.rs.core.Response response = PostGobiOrdersResponse
            .withXmlCreated(GobiResponseWriter.getWriter().write(gobiResponse));
          AsyncResult<javax.ws.rs.core.Response> result = Future.succeededFuture(response);
          asyncResultHandler.handle(result);

        }).exceptionally(helper::handleError);
      }).exceptionally(helper::handleError);
    }).exceptionally(helper::handleError);
  }

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_HEADER_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  @Override
  public void postGobiValidate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    BinaryOutStream binaryOutStream = new BinaryOutStream();
    binaryOutStream.setData(POST_DATA.getBytes(StandardCharsets.UTF_8));

    asyncResultHandler.handle(Future.succeededFuture(PostGobiValidateResponse.withXmlOK(binaryOutStream)));
  }
}
