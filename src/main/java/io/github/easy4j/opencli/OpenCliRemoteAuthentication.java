package io.github.easy4j.opencli;

import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;

/** A caller-supplied credential for an Agent's explicitly supported HTTP header. */
@Getter
public final class OpenCliRemoteAuthentication {

    private static final Pattern HEADER_NAME =
        Pattern.compile("(?:Authorization|Api-Key|X-[A-Za-z0-9][A-Za-z0-9-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_VALUE = Pattern.compile("[!-~](?:[ -~]*[!-~])?");

    private final String headerName;
    @Getter(AccessLevel.NONE)
    private final String headerValue;

    /**
     * @param headerName Authorization, Api-Key or a custom X- header
     * @param headerValue complete printable ASCII credential value, including any scheme
     */
    public OpenCliRemoteAuthentication(String headerName, String headerValue) {
        if (Objects.isNull(headerName) || !HEADER_NAME.matcher(headerName).matches()) {
            throw new IllegalArgumentException("Unsupported remote authentication header name");
        }
        if (Objects.isNull(headerValue) || !HEADER_VALUE.matcher(headerValue).matches()) {
            throw new IllegalArgumentException("Invalid remote authentication header value");
        }
        this.headerName = headerName;
        this.headerValue = headerValue;
    }
    /** @return the raw credential for the explicitly configured HTTP request */
    public String headerValue() {
        return headerValue;
    }
}
