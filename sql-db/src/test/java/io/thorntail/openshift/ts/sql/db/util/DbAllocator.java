package io.thorntail.openshift.ts.sql.db.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public final class DbAllocator {
    private final String url;

    public DbAllocator(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("DbAllocator URL must be provided");
        }
        this.url = url;
    }

    public Properties allocate(String label) throws Exception {
        String requestee = "Thorntail-TS";
        StackTraceElement[] stack = new Throwable().getStackTrace();
        if (stack.length > 1) {
            StackTraceElement caller = stack[1]; // [0] = DbAllocator.allocate
            requestee += "-" + caller.getClassName() + "." + caller.getMethodName();
        }

        Map<String, String> params = new HashMap<>();

        params.put("expression", label);
        params.put("requestee", requestee);
        params.put("expiry", "60");

        try (InputStream is = performOperation("allocate", params)) {
            Properties props = new Properties();
            props.load(is);

            if (props.toString().contains("ResourceNotAvailableException")) {
                throw new RuntimeException("ResourceNotAvailableException: No resources found for allocation with label expression: " + label);
            }

            return props;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read DbAllocator response", ex);
        }
    }

    public void erase(String uuid) throws Exception {
        performOperation("erase", Collections.singletonMap("uuid", uuid));
    }

    public void free(String uuid) throws Exception {
        performOperation("dealloc", Collections.singletonMap("uuid", uuid));
    }

    private InputStream performOperation(String operation, Map<String, String> parameters) throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(formatUrl(operation, parameters));

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            return entity.getContent();
        }

        return null;
    }

    private String formatUrl(String operation, Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(url).append("?");
        sb.append("operation=").append(operation);

        if (parameters != null) {
            for (Entry<String, String> parameter : parameters.entrySet()) {
                sb.append("&").append(parameter.getKey()).append("=").append(parameter.getValue());
            }
        }

        return sb.toString();
    }
}
