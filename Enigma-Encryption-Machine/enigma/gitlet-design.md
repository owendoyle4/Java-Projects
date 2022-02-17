# Gitlet Design Document

**Name**: Owen Doyle

## Classes and Data Structures
### Commits
- Instance Variables
  - Timestamp: A java Date object representing when the commit was made.
  - Log message: A String containing the user's message for this commit.
  - Parent: The string of the unique SHA-1 ID of this commit's parent.
  - TrackedFiles: A HashMap of the files tracked by this commit. The keys are the filenames and the values are the SHA-1 IDs.

### Command Class
  - Static Variables
    - CWD: A file representing the current working directory.
    - GITLET_FOLDER: A file representing the .gitlet folder, which houses all the following files & folders.
    - COMMIT_DIR: A file representing the commits folder, which stores all commits.
    - STAGING_AREA: A file representing the stagingArea folder.
    - STAGING_AREA_ADDITION: A file representing the stagedForAddition folder within stagingArea.
    - STAGING_AREA_REMOVAL:  A file representing the stagedForRemoval folder within stagingArea.
    - BRANCHES: A file representing the branches folder.
    - MASTER: A file representing the master branch.
    - BLOBS: A file representing the folder blobs, which stores all file contents named after their SHA-1 ID.
    - HEAD: A file representing the head pointer, which evaluates to the path of the current branch.



## Algorithms
### CommandClass
- init()
  - Makes directories (all static variables in commandClass)
  - Creates and saves the initial commit in COMMIT_DIR
  - Initialize the master branch and head.
- add(String filename)
  - Add the file to stagedForAddition
  - Checks if the same version of the file is already tracked by the head commit. If yes, remove the file from stagedForAddition.
  - Checks if the file is already in stagedForRemoval. If yes, remove it from stagedForRemoval (keeping it in stagedForAddition).
- removeFromArea(String filename, File area)
  - Remove the file named FILENAME from the folder named AREA.
- remove(String filename)
  - Checks if the file is present in stagedForAddition. If yes, remove it.
  - Checks if the file is stored in the head commit's trackedFiles variable. If yes, add file to stagedForRemoval and remove the file from the CWD.
- commit(String filename)
  - Read from my pc the head commit object and staging area.
  - Clone the head commit.
  - Modify its message and timestamp according to user input.
  - Use the staging area in order to modify the files tracked by the new commit.
  - Write back any new objects made or any modified objects read earlier (head).
  - Clear the staging area.
- checkout(String filename)
  - Evaluate head to get the head commit of the current branch's ID (commitID).
  - Call checkout(filename, commitID).
- checkout(String filename, String commitID)
  - Find the commit with commitID in the commits folder.
  - For this commit, find the ID under the key filename in the trackedFiles HashMap.
  - Find the blob with the corresponding ID.
  - Replace the content's of the file name filename in the CWD with the blob.
- log()
  - Start at the head commit of the current branch (HEAD). Display the ID, timestamp, and message.
  - Move to the current commit's parent and repeat the first step until you reach the initial commit.
- global_log()
  - Iterate through every commit in the commits folder and display the ID, timestamp, and message (order is irrelevant).

### Commit
- addFile(String filename)
  - Add the filename and SHA-1 ID to the trackedFiles HashMap.
- removeFile(String filename)
  - Remove the filename and SHA-1 ID from the trackedFiles HashMap.
- replaceFile(String filename, String newID)
  - Replace the values for under key for filename with newID.

## Persistence
### Gitlet directory
- commits
  - A folder containing all commits as files. The SHA-1 ID of a commit is the name of its file.
- blobs
  - A folder containing all blobs as files. The SHA-1 ID of a blob is the name of its file.
- stagingArea
  - stagedForAddition
    - A folder containing all blobs that will be tracked in the next commit.
  - stagedForRemoval
    - A folder containing all blobs that will no longer be tracked in the next commit.
- branches
  - A folder containing all head commits of all branches. These are stored as files where the file name is the branch name and the content is a string of the head commit's SHA-1 ID.
- head
  - A string of the SHA-1 ID for the commit at the head of the current branch



### Persistent information from user commands 
- java gitlet.Main init
  - .gitlet, commits, blobs, stagingArea, stagedForAddition, and stagedForRemoval are created in CWD.
  - The initial commit is saved in commits.
  - branches is created
    - A file named "master" with the initial commit's SHA-1 ID is saved in branches.
  - head is created, containing the working directory to master
- java gitlet.Main add [filename]
  - In blobs: filename's blob is serialized and saved in a file named after it's SHA-1 ID.
  - In stagedForAddition: filename --> SHA-1 ID
- java gitlet.Main commit -m "[message]"
  - Commit cloned from head commit.
  - Stores the filenames and SHA-1 IDs (together) of the files in stagedForAddition.
  - Removes the filenames and SHA-1 IDs (together) of the files in stagedForRemoval.
  - Head commit's SHA-1 ID saved as parent.
  - Update the current branch to this commit's SHA-1 ID.
  - Update head file in .gitlet to this commit’s SHA-1 ID.
- java gitlet.Main checkout -- [filename]
  - Copies the blob corresponding to filename from the head commit and moves it into the CWD, overwriting any file that is also named filename if it exists.
- java gitlet.Main checkout [commitID] -- [filename]
  - Copies the blob corresponding to filename from the commit with commitID and moves it into the CWD, overwriting any file that is also named filename if it exists.




