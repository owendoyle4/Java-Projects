package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Owen Doyle
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        switch (args[0]) {
        case "init":
            if (args.length > 1) {
                exitWithError("Incorrect operands.");
            }
            try {
                CommandClass.init();
            } catch (IOException error) {
                exitWithError("IO Exception Error");
            } catch (GitletException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "add":
            try {
                CommandClass.add(args[1]);
            } catch (GitletException | IOException addError) {
                exitWithError(addError.getMessage());
            }
            break;
        case "commit":
            try {
                CommandClass.commit(args[1]);
            } catch (GitletException | IOException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "checkout":
            try {
                if (args.length == 2) {
                    CommandClass.checkoutBranch(args[1]);
                } else if (args[1].equals("--") && args.length == 3) {
                    CommandClass.checkout(args[2]);
                } else if (args[2].equals("--") && args.length == 4) {
                    CommandClass.checkout(args[1], args[3]);
                } else {
                    exitWithError("Incorrect operands.");
                }
            } catch (GitletException | IOException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "rm":
            try {
                if (args.length == 2) {
                    CommandClass.rm(args[1]);
                }
            } catch (GitletException | IOException error) {
                exitWithError(error.getMessage());
            }
            break;
        default:
            main2(args);
        }
        return;
    }

    /** Second batch of user commands.
     * Usage: java gitlet.Main ARGS, where ARGS contains
     *      *  <COMMAND> <OPERAND> .... */
    public static void main2(String... args) {
        switch (args[0]) {
        case "log":
            if (args.length == 1) {
                CommandClass.log();
            }
            break;
        case "global-log":
            if (args.length == 1) {
                CommandClass.globalLog();
            }
            break;
        case "find":
            try {
                if (args.length == 2) {
                    CommandClass.find(args[1]);
                }
            } catch (GitletException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "status":
            try {
                if (args.length == 1) {
                    CommandClass.status();
                }
            } catch (GitletException error) {
                exitWithError(error.getMessage());
            }
            break;
        default:
            main3(args);
        }
        return;
    }

    /** Third batch of user commands.
     * Usage: java gitlet.Main ARGS, where ARGS contains
     *      *  <COMMAND> <OPERAND> .... */
    public static void main3(String... args) {
        switch (args[0]) {
        case "branch":
            try {
                if (args.length == 2) {
                    CommandClass.branch(args[1]);
                }
            } catch (IOException | GitletException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "rm-branch":
            try {
                if (args.length == 2) {
                    CommandClass.rmBranch(args[1]);
                }
            } catch (GitletException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "reset":
            try {
                if (args.length == 2) {
                    CommandClass.reset(args[1]);
                }
            } catch (GitletException | IOException error) {
                exitWithError(error.getMessage());
            }
            break;
        case "merge":
            try {
                if (args.length == 2) {
                    CommandClass.merge(args[1]);
                }
            } catch (GitletException | IOException error) {
                exitWithError(error.getMessage());
            }
            break;
        default:
            exitWithError("No command with that name exists.");
        }
        return;
    }

    /** Print MESSAGE in the terminal and exit the execution. */
    public static void exitWithError(String message) {
        Utils.message(message);
        System.exit(0);
    }

}
