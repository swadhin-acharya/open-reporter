package io.github.swadhinsoft.openreporter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents an artifact attached to a test result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentModel {

    private String name;
    private String mimeType;
    private String content;
    private String filePath;
    private long   timestamp;

    /** Required for Jackson deserialisation. */
    public AttachmentModel() {}

    public AttachmentModel(String name, String mimeType, String content, String filePath) {
        this.name      = name;
        this.mimeType  = mimeType;
        this.content   = content;
        this.filePath  = filePath;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName()      { return name; }
    public String getMimeType()  { return mimeType; }
    public String getContent()   { return content; }
    public String getFilePath()  { return filePath; }
    public long   getTimestamp() { return timestamp; }

    public void setName(String v)      { this.name = v; }
    public void setMimeType(String v)  { this.mimeType = v; }
    public void setContent(String v)   { this.content = v; }
    public void setFilePath(String v)  { this.filePath = v; }
    public void setTimestamp(long v)   { this.timestamp = v; }
}
