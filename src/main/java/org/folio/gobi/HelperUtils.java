package org.folio.gobi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.folio.gobi.exceptions.HttpException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class HelperUtils {

  private HelperUtils() {

  }

  public static String truncate(String message, int limit) {
    return (message != null && limit > 0) ? message.substring(0, Math.min(message.length(), limit)) : message;
  }

  public static JsonObject verifyAndExtractBody(org.folio.rest.tools.client.Response response) {
    if (response == null) {
      throw new CompletionException(new NullPointerException("response is null"));
    }

    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError().toString()));
    }

    return response.getBody();
  }

  public static String extractLocationId(JsonObject obj) {
    return extractIdOfFirst(obj, "locations");
  }

  public static List<String> extractMaterialTypeId(JsonObject obj) {
    // for now GOBI has only a single material type, handle multiple types in
    // future
    List<String> materialTypeList = new ArrayList<>();
    materialTypeList.add(extractIdOfFirst(obj, "mtypes"));
    return materialTypeList;
  }

  public static String extractVendorId(JsonObject obj) {
    return extractIdOfFirst(obj, "vendors");
  }

  public static String extractWorkflowStatusId(JsonObject obj) {
    return extractIdOfFirst(obj, "workflow_statuses");
  }

  public static String extractReceiptStatusId(JsonObject obj) {
    return extractIdOfFirst(obj, "receipt_statuses");
  }

  public static String extractPaymentStatusId(JsonObject obj) {
    return extractIdOfFirst(obj, "payment_statuses");
  }

  public static String extractActivationStatusId(JsonObject obj) {
    return extractIdOfFirst(obj, "activation_statuses");
  }

  public static String extractIdOfFirst(JsonObject obj, String arrField) {
    if (obj == null || arrField == null || arrField.isEmpty()) {
      return null;
    }
    JsonArray jsonArray = obj.getJsonArray(arrField);
    if (jsonArray == null || jsonArray.size() == 0 ) {
      return null;
    }
    JsonObject item = jsonArray.getJsonObject(0);
    if (item == null) {
      return null;
    }
    return item.getString("id");
  }

  public static String encodeValue(String value) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
  }

}
