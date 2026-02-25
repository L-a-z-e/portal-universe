package com.portal.universe.event.drive;

/**
 * Drive 도메인 Kafka topic 상수.
 *
 * <p>SSOT: {@code services/event-contracts/schemas/com/portal/universe/event/drive/*.avsc}</p>
 */
public final class DriveTopics {

    public static final String FILE_UPLOADED = "drive.file.uploaded";
    public static final String FILE_DELETED = "drive.file.deleted";
    public static final String FOLDER_CREATED = "drive.folder.created";

    private DriveTopics() {}
}
