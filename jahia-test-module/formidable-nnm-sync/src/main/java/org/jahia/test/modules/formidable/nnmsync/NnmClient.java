package org.jahia.test.modules.formidable.nnmsync;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The two calls to the NNM system, with the operator's URLs and timeouts, both bounded: a system that
 * does not answer costs a thread of this module for at most the request timeout, never more.
 * The URLs are the operator's (a file of {@code karaf/etc}), never the content's, so nothing an author
 * types can send a submission somewhere else.
 */
@Component(service = NnmClient.class, configurationPid = NnmClient.PID, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@Designate(ocd = NnmClient.Settings.class)
public class NnmClient {

    static final String PID = "org.jahia.test.modules.formidable.nnmsync";

    @ObjectClassDefinition(name = "Formidable NNM sync", description = "Where the NNM system receives a form's configuration and its submissions.")
    @interface Settings {

        @AttributeDefinition(name = "Configuration URL", description = "Receives a form's configuration (id, version, fields) at each publication.")
        String configUrl() default "http://localhost:9090/api/form-configs";

        @AttributeDefinition(name = "Submission URL", description = "Receives each submission of a connected form.")
        String submissionUrl() default "http://localhost:9090/api/form-submissions";

        @AttributeDefinition(name = "Connect timeout (seconds)")
        int connectTimeoutSeconds() default 3;

        @AttributeDefinition(name = "Request timeout (seconds)", description = "Time allowed to get the whole answer, once the request is sent.")
        int requestTimeoutSeconds() default 10;
    }

    private volatile URI configUri;
    private volatile URI submissionUri;
    private volatile Duration requestTimeout;
    private volatile HttpClient client;

    @Activate
    @Modified
    void configure(Settings settings) {
        configUri = URI.create(settings.configUrl());
        submissionUri = URI.create(settings.submissionUrl());
        requestTimeout = Duration.ofSeconds(Math.max(1, settings.requestTimeoutSeconds()));
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, settings.connectTimeoutSeconds())))
                .build();
    }

    /** The status the system answered to a form's configuration. */
    int postConfig(String json) throws IOException {
        return post(configUri, json);
    }

    /** The status the system answered to a submission. */
    int postSubmission(String json) throws IOException {
        return post(submissionUri, json);
    }

    private int post(URI uri, String json) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while calling " + uri, e);
        }
    }
}
