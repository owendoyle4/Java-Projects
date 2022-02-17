package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

/** Represent a gitlet commit.
 * @author Owen Doyle*/
public class Commit implements Serializable {

    /** The current working directory file. */
    static final File CWD = new File(".");

    /** This commit's message. */
    private String _message;

    /** This commit's time stamp. */
    private Date _timestamp;

    /** This commit's parent's ID. */
    private String _parent;

    /** This commit's merge parent's ID. */
    private String _mergeParent;

    /** This commit's mapping of tracked files. */
    private TreeMap<String, String> _trackedFiles;

    /** Create a new Commit object with a MESSAGE, PARENT, and MERGEPARENT. */
    public Commit(String message, String parent, String mergeParent) {
        this._message = message;
        this._parent = parent;
        this._mergeParent = mergeParent;
        if (parent == null) {
            this._timestamp = defaultDate;
            this._trackedFiles = new TreeMap<String, String>();
        } else {
            this._timestamp = new Date();
            File parentFile = Utils.join(CommandClass.COMMIT_DIR, parent);
            Commit parentCommit = Utils.readObject(parentFile, Commit.class);
            this._trackedFiles = parentCommit.getTrackedFiles();
        }
    }

    /** Get the time stamp of this commit.
     * @return _timestamp */
    public String getTimestamp() {
        return simpleDateFormat.format(_timestamp);
    }

    /** Get the message of this commit.
     * @return _message */
    public String getMessage() {
        return _message;
    }

    /** Get the parent ID of this commit.
     * @return _parent */
    public String getParentID() {
        return _parent;
    }

    /** Get the merge parent ID of this commit.
     * @return _mergeParent */
    public String getMergeParentID() {
        return _mergeParent;
    }

    /** Get the tracked files of this commit.
     * @return _trackedFiles */
    TreeMap<String, String> getTrackedFiles() {
        return this._trackedFiles;
    }

    /** Return the ID of the blob named FILENAME in this commit.
     * Return null if FILENAME  is not tracked.
     * @return blob ID for FILENAME */
    String getBlobID(String filename) {
        return _trackedFiles.get(filename);
    }

    /** Add the key value pair <FILENAME, BLOBID> to be tracked by commit. */
    void addFile(String filename, String blobID) {
        _trackedFiles.put(filename, blobID);
    }

    /** Remove the key value pair <FILENAME, BLOBID> from this commit. */
    void removeFile(String filename, String blobID) {
        _trackedFiles.remove(filename);
    }

    /** Return whether FILENAME is tracked by this commit.*/
    Boolean tracksFile(String filename) {
        return getTrackedFiles().containsKey(filename);
    }

    /** The default data for all initial commits. */
    private Date defaultDate = new Date(0);

    /** Pattern to make the date print statements. */
    private String datePattern = "EEE MMM d HH:mm:ss yyyy Z";

    /** An instance of SimpleDateFormat that uses the commit pattern. */
    private SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat(datePattern);
}
