package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** All commands and logic for gitlet repository work is housed here.
 * @author Owen Doyle */
public class CommandClass {

    /** A file representing the current working directory. */
    static final File CWD = new File(".");

    /** A file representing the .gitlet folder, which
     * houses all the following files & folders. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** A file representing the commits folder,
     *  which stores all commits. */
    static final File COMMIT_DIR =
            Utils.join(GITLET_FOLDER, "commits");

    /** A file representing the folder blobs, which stores
     * all file contents named after their SHA-1 ID. */
    static final File BLOBS = Utils.join(GITLET_FOLDER, "blobs");

    /** A file representing the stagingArea folder. */
    static final File STAGING_AREA =
            Utils.join(GITLET_FOLDER, "stagingArea");

    /** A file representing the stagedForAddition
     *  folder within stagingArea. */
    static final File STAGING_AREA_ADDITION =
            Utils.join(STAGING_AREA, "stagedForAddition");

    /** A file representing the stagedForRemoval
     * folder within stagingArea. */
    static final File STAGING_AREA_REMOVAL =
            Utils.join(STAGING_AREA, "stagedForRemoval");

    /** A file representing the branches folder. */
    static final File BRANCHES =
            Utils.join(GITLET_FOLDER, "branches");

    /** A file representing the master branch. */
    private static File master;

    /** A file representing the head pointer, which
     *  evaluates to the path of the current branch. */
    private static File head = new File(GITLET_FOLDER, "head");

    /** Creates a new Gitlet version-control system in the current
     *  directory. This system will automatically start with one
     *  commit: a commit that contains no files and has the commit
     * message initial commit (just like that, with no punctuation).
     * It will have a single branch: master, which initially points
     * to this initial commit, and master will be the current branch.
     * The timestamp for this initial commit will be 00:00:00 UTC,
     * Thursday, 1 January 1970 in whatever format you choose for
     * dates (this is called "The (Unix) Epoch", represented
     * internally by the time 0.) Since the initial commit in all
     * repositories created by Gitlet will have exactly the same
     * content, it follows that all repositories will automatically
     * share this commit (they will all have the same UID) and all
     * commits in all repositories will trace back to it. */
    public static void init() throws IOException, GitletException {
        if (GITLET_FOLDER.exists()) {
            throw Utils.error("A Gitlet version-control system"
                    + " already exists in the current directory.");
        }
        GITLET_FOLDER.mkdirs();

        COMMIT_DIR.mkdirs();
        BLOBS.mkdirs();
        STAGING_AREA.mkdirs();
        STAGING_AREA_ADDITION.mkdirs();
        STAGING_AREA_REMOVAL.mkdirs();

        Commit initialCommit = new Commit("initial commit",
                null, null);
        byte [] initialCommitBytes = Utils.serialize(initialCommit);
        String initialCommitID = Utils.sha1(initialCommitBytes);

        File initialCommitFile = Utils.join(COMMIT_DIR, initialCommitID);
        initialCommitFile.createNewFile();
        Utils.writeObject(initialCommitFile, initialCommit);

        BRANCHES.mkdirs();
        master = new File(BRANCHES, "master");
        master.createNewFile();
        Utils.writeObject(master, initialCommitID);

        head.createNewFile();
        Utils.writeObject(head, master.toString());
    }

    /** Adds a copy of the file as it currently exists to the staging area
     * (see the description of the commit command). For this reason, adding
     * a file is also called staging the file for addition. Staging an
     * already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere in .gitlet.
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there (as can happen when a file is
     * changed, added, and then changed back). The file will no longer be
     * staged for removal (see gitlet rm), if it was at the time of the
     * command.
     * @param filename is the filename.*/
    public static void add(String filename)
            throws GitletException, IOException {
        File file = new File(CWD, filename);
        if (file.exists()) {
            byte[] fileBytes = Utils.readContents(file);
            String blobID = Utils.sha1(fileBytes);

            File fileInRA = new File(STAGING_AREA_REMOVAL, filename);
            if (fileInRA.exists()) {
                removeFromArea(filename, STAGING_AREA_REMOVAL);
            } else {
                Commit headCommit = getHeadCommit();
                String fileID = headCommit.getBlobID(filename);
                if (fileID == null || !fileID.equals(blobID)) {
                    addToBlobsFolder(blobID, fileBytes);
                    stageFile(STAGING_AREA_ADDITION, filename, blobID);
                }
            }

        } else {
            throw new GitletException("File does not exist.");
        }
    }

    /** Add a file to the blobs folder with name BLOBID and contents
     * FILEBYTES. Will not create duplicates if a file with BLOBID
     * already exists. Throws IOEXCEPTION if new file cannot be created. */
    public static void addToBlobsFolder(String blobID,
                                        byte[] fileBytes) throws IOException {
        File fileInBlobs = new File(BLOBS, blobID);
        if (!fileInBlobs.exists()) {
            fileInBlobs.createNewFile();
            Utils.writeContents(fileInBlobs, fileBytes);
        }
    }

    /** Add a file with name FILENAME and contents BLOBID to the indicated
     * staging area DIR (addition or removal). If the file was previously
     * staged, override it. If the file was not staged, make a new file. */
    public static void stageFile(File dir, String filename,
                                 String blobID) throws IOException {
        File fileInSA = new File(dir, filename);
        if (!fileInSA.exists()) {
            fileInSA.createNewFile();
        }
        Utils.writeContents(fileInSA, Utils.serialize(blobID));
    }

    /** Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file
     * from the working directory if the user has not already done so (do not
     * remove it unless it is tracked in the current commit).
     * @param  filename is the file name. */
    public static void remove(String filename) {

    }

    /** Remove the file named FILENAME from the folder named AREA. */
    public static void removeFromArea(String filename, File area) {
        Utils.join(area, filename).delete();
    }

    /** Creates a commit with MESSAGE and no merge parent. */
    public static void commit(String message)
            throws IOException, GitletException {
        commitLogic(message, null);
    }

    /** Saves a snapshot of tracked files in the current commit and staging
     * area so they can be restored at a later time, creating a new commit.
     * The commit is said to be tracking the saved files. By default, each
     * commit's snapshot of files will be exactly the same as its parent
     * commit's snapshot of files; it will keep versions of files exactly as
     * they are, and not update them. A commit will only update the contents
     * of files it is tracking that have been staged for addition at the time
     * of commit, in which case the commit will now include the version of the
     * file that was staged instead of the version it got from its parent. A
     * commit will save and start tracking any files that were staged for
     * addition but weren't tracked by its parent. Finally, files tracked in
     * the current commit may be untracked in the new commit as a result being
     * staged for removal by the rm command (below).
     * @param message is the commit's message.
     * @param mergeParent is the merge parent of the commit. */
    public static void commitLogic(String message, String mergeParent)
            throws IOException, GitletException {

        if (message.equals("")) {
            throw Utils.error("Please enter a commit message.");
        }

        String headCommitID = getHeadCommitID();
        Commit newCommit = new Commit(message, headCommitID, mergeParent);
        String newCommitID = getCommitID(newCommit);

        processStagingArea(newCommit);
        updateCurrBranchCommit(newCommitID);
        writeToFolder(COMMIT_DIR, newCommit, newCommitID);
    }



    /** Takes the version of the file with FILENAME as it
     *  exists in the head commit, the front of the current
     *  branch, and puts it in the working directory,
     *  overwriting the version of the file that's already
     *  there if there is one. The new version of the file
     *  is not staged. */
    public static void checkout(String filename) {
        checkout(getHeadCommitID(), filename);
    }

    /** The length of the SHA-1 ID for a commit. */
    private static final int FULLIDLENGTH = 40;

    /** Takes the version of the file with FILENAME as it exists in
     *  the commit with COMMITID, and puts it in the working directory,
     *  overwriting the version of the file that's already there
     *  if there is one. The new version of the file is not staged. */
    public static void checkout(String commitID, String filename) {
        if (commitID.length() < FULLIDLENGTH) {
            for (String fullID : Utils.plainFilenamesIn(COMMIT_DIR)) {
                if (fullID.startsWith(commitID)) {
                    commitID = fullID;
                    break;
                }
            }
        }

        File commitFile = Utils.join(COMMIT_DIR, commitID);
        if (!commitFile.exists()) {
            throw new GitletException("No commit with that id exists.");
        }
        Commit c = Utils.readObject(commitFile, Commit.class);

        String blobID = c.getBlobID(filename);
        if (blobID == null) {
            throw new GitletException("File does not exist in that commit.");
        }

        File blobFile = Utils.join(BLOBS, blobID);

        byte[] blob = Utils.readContents(blobFile);
        Utils.writeContents(Utils.join(CWD, filename), blob);
    }

    /** Calls checkoutBranch with the corresponding file of BRANCHNAME. */
    public static void checkoutBranch(String branchName)
            throws IOException, GitletException {
        File branchFile = Utils.join(BRANCHES, branchName);
        checkoutBranch(branchFile);
    }

    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions
     * of the files that are already there if they exist. Also, at the
     * end of this command, the given branch will now be considered the
     * current branch (HEAD). Any files that are tracked in the current
     * branch but are not present in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch is the
     * current branch, given as BRANCHFILE. */
    public static void checkoutBranch(File branchFile)
            throws IOException, GitletException {
        if (!branchFile.exists()) {
            throw Utils.error("No such branch exists.");
        }

        String headBranchPath = Utils.readObject(head, String.class);
        File headCommitFile = new File(headBranchPath);
        if (branchFile.equals(headCommitFile)) {
            throw Utils.error("No need to checkout the current branch.");
        }

        checkForUntrackedFiles();
        clearCDW();
        loadBranch(branchFile);
        Utils.writeObject(head, branchFile.toString());
    }

    /** Clears the CWD. */
    public static void clearCDW() {
        for (String filename : Utils.plainFilenamesIn(CWD)) {
            File file = Utils.join(CWD, filename);
            file.delete();
        }
    }

    /** Copy all files from the head commit of BRANCHFILE into the CWD. */
    public static void loadBranch(File branchFile) throws IOException {
        String commitID = Utils.readObject(branchFile, String.class);
        Commit c = getCommit(commitID);
        TreeMap<String, String> trackedFiles = c.getTrackedFiles();
        for (Map.Entry<String, String> entry : trackedFiles.entrySet()) {
            String filename = entry.getKey();
            String blobID = entry.getValue();
            File blobFile = Utils.join(BLOBS, blobID);

            File fileCWD = Utils.join(CWD, filename);
            fileCWD.createNewFile();
            Utils.writeContents(fileCWD, Utils.readContents(blobFile));

        }
    }

    /** A file is tracked if it is accounted for in the current commit.
     * A file is untracked if it’s in the CWD but isn’t staged to be
     * added and it’s not accounted for in the current commit */
    public static void checkForUntrackedFiles() throws GitletException {
        Commit currCommit = getHeadCommit();
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!Utils.join(STAGING_AREA_ADDITION, fileName).exists()
                    && !currCommit.tracksFile(fileName)) {
                throw Utils.error("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
            }
        }
    }

    /** Unstage the file with FILENAME if it is currently staged
     *  for addition. If the file is tracked in the current commit,
     *  stage it for removal and remove the file from the working
     *  directory if the user has not already done so (do not
     *  remove it unless it is tracked in the current commit). */
    public static void rm(String filename) throws IOException, GitletException {
        Boolean fileWasStaged = Utils.join(STAGING_AREA_ADDITION,
                filename).delete();

        if (!fileWasStaged && getHeadCommit().tracksFile(filename)) {
            String blobID = getHeadCommit().getBlobID(filename);
            stageFile(STAGING_AREA_REMOVAL, filename, blobID);
            File fileInCWD = Utils.join(CWD, filename);
            fileInCWD.delete();
        } else if (!fileWasStaged && !getHeadCommit().tracksFile(filename)) {
            throw Utils.error("No reason to remove the file.");
        }
    }

    /** Displays what branches currently exist, and marks the current
     *  branch with a *. Also displays what files have been staged
     *  for addition or removal. An example of the exact format it
     *  should follow is as follows. */
    public static void status() throws GitletException {
        if (!GITLET_FOLDER.exists()) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }

        String headCommitID = getHeadCommitID();

        System.out.println("=== Branches ===");
        for (String branchName : Utils.plainFilenamesIn(BRANCHES)) {
            File branchPath = Utils.join(BRANCHES,  branchName);
            if (Utils.readObject(branchPath,
                    String.class).equals(headCommitID)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String fileName : Utils.plainFilenamesIn(STAGING_AREA_ADDITION)) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName : Utils.plainFilenamesIn(STAGING_AREA_REMOVAL)) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
    }


    /** Starting at the current head commit, display information about
     *  each commit backwards along the commit tree until the initial
     *  commit, following the first parent commit links, ignoring any
     *  second parents found in merge commits. (In regular Git, this is
     *  what you get with git log --first-parent). This set of commit
     *  nodes is called the commit's history. For every node in this
     *  history, the information it should display is the commit id,
     *  the time the commit was made, and the commit message. */
    public static void log() {
        String currCommitID = getHeadCommitID();
        Commit curr = getCommit(currCommitID);

        while (curr != null) {
            System.out.println("===");
            System.out.println(logString(curr, currCommitID));
            System.out.println();

            currCommitID = curr.getParentID();
            curr = getCommit(currCommitID);
        }
    }

    /** Like log, except displays information about all commits ever made.
     *  The order of the commits does not matter. */
    public static void globalLog() {
        for (String currCommitID : Utils.plainFilenamesIn(COMMIT_DIR)) {
            Commit curr = getCommit(currCommitID);
            System.out.println("===");
            System.out.println(logString(curr, currCommitID));
            System.out.println();
        }
    }

    /** Return the proper logstring for commit C,
     * factoring in it's COMMITID. */
    public static String logString(Commit c, String commitID) {
        if (c.getMergeParentID() == null) {
            return "commit " + commitID + "\n"
                    + "Date: " + c.getTimestamp()
                    + "\n" + c.getMessage();
        } else {
            return "commit " + commitID + "\n"
                    + "Merge: " + c.getParentID().substring(0, 7)
                    + " " + c.getMergeParentID().substring(0, 7) + "\n"
                    + "Date: " + c.getTimestamp() + "\n"
                    + c.getMessage();
        }
    }

    /** Prints out the ids of all commits that have COMMITMESSAGE,
     * one per line. If there are multiple such commits, it prints the ids out
     * on separate lines. The commit message is a single operand; to indicate
     * a multiword message, put the operand in quotation marks, as for the
     * commit command above.*/
    public static void find(String commitMessage) throws GitletException {
        Boolean noSuchCommit = true;
        for (String currCommitID : Utils.plainFilenamesIn(COMMIT_DIR)) {
            Commit curr = getCommit(currCommitID);
            if (curr.getMessage().equals(commitMessage)) {
                System.out.println(currCommitID);
                noSuchCommit = false;
            }
        }
        if (noSuchCommit) {
            throw Utils.error("Found no commit with that message.");
        }

    }

    /** Creates a new branch with the BRANCHNAME, and points
     * it at the current head node. A branch is nothing more
     * than a name for a reference (a SHA-1 identifier) to a
     * commit node. This command does NOT immediately switch
     * to the newly created branch (just as in real Git).
     * Before you ever call branch, your code should be
     * running with a default branch called "master". */
    public static void branch(String branchName)
            throws IOException {
        File branchFile = Utils.join(BRANCHES, branchName);
        if (branchFile.exists()) {
            throw Utils.error("A branch with that name already exists.");
        }
        branchFile.createNewFile();
        Utils.writeObject(branchFile, getHeadCommitID());
    }

    /** Deletes the branch with the name BRANCHNAME. This only
     *  means to delete the pointer associated with the branch;
     *  it does not mean to delete all commits that were
     *  created under the branch, or anything like that. */
    public static void rmBranch(String branchName) throws GitletException {
        File branchFile = Utils.join(BRANCHES, branchName);

        if (!branchFile.exists()) {
            throw Utils.error("A branch with that name does not exist.");
        }

        String headBranchCommitID = Utils.readObject(branchFile, String.class);
        if (getHeadCommitID().equals(headBranchCommitID)) {
            throw Utils.error("Cannot remove the current branch.");
        }
        branchFile.delete();
    }

    /** Checks out all the files tracked by the given COMMITID.
     * Removes tracked files that are not present in that
     * commit. Also moves the current branch's head to
     * that commit node. See the intro for an example of what
     * happens to the head pointer after using reset. The
     * [commit id] may be abbreviated as for checkout. The
     * staging area is cleared. The command is essentially
     * checkout of an arbitrary commit that also changes the
     * current branch head.*/
    public static void reset(String commitID)
            throws GitletException, IOException {
        checkForUntrackedFiles();

        File commitFile = Utils.join(COMMIT_DIR, commitID);
        if (!commitFile.exists()) {
            throw new GitletException("No commit with that id exists.");
        }

        clearCDW();
        clearStagingArea();

        String currBranchPath = Utils.readObject(head, String.class);
        File currBranch = new File(currBranchPath);

        Utils.writeObject(currBranch, commitID);

        loadBranch(currBranch);
    }

    /** Merges files from the BRANCHNAME into the current branch. */
    public static void merge(String branchName)
            throws IOException, GitletException {
        checkForUntrackedFiles();

        if (STAGING_AREA_ADDITION.listFiles().length != 0
                || STAGING_AREA_REMOVAL.listFiles().length != 0) {
            throw Utils.error("You have uncommitted changes.");
        }

        File branchFile = Utils.join(BRANCHES, branchName);
        if (!branchFile.exists()) {
            throw Utils.error("A branch with that name does not exist.");
        }

        String currBranchPath = Utils.readObject(head, String.class);
        File currBranchFile = new File(currBranchPath);
        if (currBranchFile.equals(branchFile)) {
            throw Utils.error("Cannot merge a branch with itself.");
        }

        String splitPointID = findSplitPointID(currBranchFile, branchFile);

        String branchCommitID = Utils.readObject(branchFile, String.class);
        if (splitPointID.equals(branchCommitID)) {
            throw Utils.error("Given branch is an ancestor"
                    + " of the current branch.");
        } else if (splitPointID.equals(getHeadCommitID())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
        } else {
            Commit headCommit = getHeadCommit();
            Commit other = getCommit(branchCommitID);
            Commit splitPoint = getCommit(splitPointID);

            Set allFileNames = getAllFiles(headCommit, other, splitPoint);
            applyMergeRules(allFileNames, headCommit, other, splitPoint);

            commitLogic("Merged " + branchName + " into "
                    + currBranchFile.getName() + ".", branchCommitID);
            if (encounteredConflict) {
                System.out.println("Encountered a merge conflict.");
                encounteredConflict = false;
            }
        }
    }

    /** Return one of the latest common ancestors of the commits
     *  at the heads of CURRBRANCHFILE and BRANCHFILE. */
    public static String findSplitPointID(
            File currBranchFile, File branchFile) {
        HashSet<String> otherBranchAncestry = new HashSet<>();
        String branchCommitID = Utils.readObject(branchFile, String.class);
        otherBranchAncestry.add(branchCommitID);

        Commit currCommit = getCommit(branchCommitID);
        while (currCommit != null) {
            otherBranchAncestry.add(currCommit.getParentID());
            otherBranchAncestry.add(currCommit.getMergeParentID());
            currCommit = getCommit(currCommit.getParentID());
        }

        String headCommitID = Utils.readObject(currBranchFile, String.class);
        currCommit = getCommit(headCommitID);
        if (otherBranchAncestry.contains(headCommitID)) {
            return headCommitID;
        }

        String parentID;
        String mergeParentID;
        while (currCommit != null) {
            parentID = currCommit.getParentID();
            mergeParentID = currCommit.getMergeParentID();
            if (otherBranchAncestry.contains(parentID)) {
                return parentID;
            } else if (mergeParentID != null
                    && otherBranchAncestry.contains(mergeParentID)) {
                return mergeParentID;
            }
            currCommit = getCommit(parentID);
        }
        return null;
    }

    /** Add all tracked filenames in the given COMMITS to a set.
     * Return the set called allFileNames. */
    public static Set getAllFiles(Commit... commits) {
        HashSet<String> allFileNames = new HashSet<>();
        for (Commit commit : commits) {
            for (Map.Entry<String, String>
                entry : commit.getTrackedFiles().entrySet()) {
                allFileNames.add(entry.getKey());
            }
        }
        return allFileNames;
    }

    /** Applies the merge rules, as numerated in the spec.
     * Uses information from ALLFILES, HEADCOMMIT, OTHER,
     * and SPLITCOMMIT to apply rules. */
    public static void applyMergeRules(Set allFiles, Commit headCommit,
                                       Commit other,
                                       Commit splitCommit) throws IOException {
        for (Object fileName : allFiles.toArray()) {
            String filename = (String) fileName;
            File cwdFile = Utils.join(CWD, filename);
            String vHead = headCommit.getBlobID(filename);
            if (vHead == null) {
                vHead = "";
            }
            String vOther = other.getBlobID(filename);
            if (vOther == null) {
                vOther = "";
            }
            String vSplit = splitCommit.getBlobID(filename);
            if (vSplit == null) {
                vSplit = "";
            }

            if (vSplit.equals(vHead) && !vSplit.equals(vOther)) {
                if (vOther.equals("")) {
                    rm(filename);
                } else {
                    if (!cwdFile.exists()) {
                        cwdFile.createNewFile();
                    }
                    File vOtherBlob = Utils.join(BLOBS, vOther);
                    Utils.writeContents(cwdFile,
                            Utils.readContents(vOtherBlob));
                    add(filename);
                }
            } else if (!vSplit.equals(vHead) && !vSplit.equals(vOther)
                    && !vHead.equals(vOther)) {
                conflict(filename, vHead, vOther);
                encounteredConflict = true;
            }
        }
    }

    /** Construct a new conflict file that will be used in the new commit
     * in the event of a conflict in FILENAME between VHEAD and VOTHER. */
    public static void conflict(String filename, String vHead,
                                String vOther) throws IOException {
        String headContents;
        String otherContents;
        if (Utils.join(BLOBS, vHead).isFile()) {
            headContents = Utils.readContentsAsString(Utils.join(BLOBS, vHead));
        } else {
            headContents = "";
        }

        if (Utils.join(BLOBS, vOther).isFile()) {
            otherContents = Utils.readContentsAsString(
                    Utils.join(BLOBS, vOther));
        } else {
            otherContents = "";
        }

        String newContents =
                "<<<<<<< HEAD" + "\n"
                        + headContents
                        + "=======" + "\n"
                        + otherContents
                        + ">>>>>>>" + "\n";

        Utils.writeContents(Utils.join(CWD, filename), newContents);
        add(filename);
    }


    /** Clear the addition and removal folders within the staging area. */
    public static void clearStagingArea() {
        for (String fileName : Utils.plainFilenamesIn(STAGING_AREA_ADDITION)) {
            Utils.join(STAGING_AREA_ADDITION, fileName).delete();
        }
        for (String fileName : Utils.plainFilenamesIn(STAGING_AREA_REMOVAL)) {
            Utils.join(STAGING_AREA_REMOVAL, fileName).delete();
        }
    }


    /** Return the commit with ID COMMITID. */
    public static Commit getCommit(String commitID) {
        if (commitID == null) {
            return null;
        }
        return Utils.readObject(Utils.join(COMMIT_DIR, commitID), Commit.class);
    }

    /** Return the head commit. */
    public static Commit getHeadCommit() {
        return Utils.readObject(Utils.join(COMMIT_DIR,
                getHeadCommitID()), Commit.class);
    }

    /** Update the head pointer to BRANCHNAME's path. */
    public static void setHead(String branchName) {
        File branchFile = Utils.join(BRANCHES, "branchName");
        Utils.writeObject(head, branchFile.toString());
    }

    /** Return the head commit's SHA-1 ID, as stored
     * in .gitlet/head. */
    public static String getHeadCommitID() {
        String branchFilePath = Utils.readObject(head, String.class);
        File headCommitFile = new File(branchFilePath);
        return Utils.readObject(headCommitFile, String.class);
    }


    /** Return C's commit ID. */
    public static String getCommitID(Commit c) {
        return Utils.sha1(Utils.serialize(c));
    }

    /** Return the blobID from a file with FILEPATH
     * stored in one of the staging areas. */
    public static String getBlobID(File filepath) {
        return Utils.readObject(filepath, String.class);
    }

    /** Writes object OBJ to the file named ID found in DIR. */
    public static void writeToFolder(File dir, Serializable obj,
                                     String id) throws IOException {
        File commitInCommitsFolder = Utils.join(dir, id);
        Utils.writeObject(commitInCommitsFolder, obj);
        commitInCommitsFolder.createNewFile();
    }

    /** Iterate through the two staging areas (addition and removal),
     * adding or removing files from commit C accordingly. */
    public static void processStagingArea(Commit c) throws GitletException {
        if (Utils.plainFilenamesIn(STAGING_AREA_ADDITION).isEmpty()
                && Utils.plainFilenamesIn(STAGING_AREA_REMOVAL).isEmpty()) {
            throw Utils.error("No changes added to the commit.");
        }

        for (File dir : new File[] {STAGING_AREA_ADDITION,
            STAGING_AREA_REMOVAL}) {
            for (String filename : Utils.plainFilenamesIn(dir)) {
                File file = Utils.join(dir, filename);
                String blobID = getBlobID(file);
                if (dir.equals(STAGING_AREA_ADDITION)) {
                    c.addFile(filename, blobID);
                } else {
                    c.removeFile(filename, blobID);
                }
                file.delete();
            }
        }
    }

    /** Updates the commit that is pointed to
     * as the head of the current branch to COMMITID. */
    public static void updateCurrBranchCommit(String commitID) {
        String currBranchPath = Utils.readObject(head, String.class);
        File currBranch = new File(currBranchPath);
        Utils.writeObject(currBranch, commitID);
    }

    /** Keeps track of whether the file is in conflict during merge. */
    private static Boolean encounteredConflict = false;

}
