/*
  Show Folder Size #1 - Show File Space Used for Folders and Subfolders
  Written by: Keith Fenske, http://kwfenske.github.io/
  Friday, 28 February 2014
  Java class name: ShowFolderSize1
  Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 console application to display the total size of files in
  folders named on the command line, with or without subfolders included.  The
  TEMP folder in the following example has 5 files plus 19 more in three
  subfolders, for a total of 24 files:

      java  ShowFolderSize1  D:\TEMP

      TEMP = 207.3 MB in 24 files and 3 subfolders.
        BACKUP = 176.7 MB in 9 files.
        DOWNLOAD = 80.9 KB in 8 files.
        UPLOAD = 196.6 KB in 2 files.

  Redirect output to a text file, and save as CSV if desired (comma-separated
  values).  Options go before folder names on the command line:

      -? = -help = show summary of command-line syntax
      -b -kb -mb -gb -tb = show sizes in kilobytes, megabytes, etc.
      -c0 = ignore uppercase/lowercase in subfolder names (default)
      -c1 = -c = strict Unicode order for case in subfolder names
      -i# = incremental left indent for subfolders; default is -i2
      -m0 = report details for each folder, but don't add subfolders
      -m1 = report details for each folder plus subfolders (default)
      -m2 = show excessive details about every folder and subfolder
      -r# = maximum subfolder depth to report; default is -r9
      -s# = maximum subfolder depth to search; default is -s99
      -v0 = output as formatted text with -b or -m options (default)
      -v1 = -v = output as raw comma-separated values (see source code)

  There is no graphical interface (GUI) for this program; it must be run from a
  command prompt, command shell, or terminal window.  Folders are reported in
  the same order as they are given on the command line.  The order of
  subfolders depends upon the "case" option.  The character set for folder
  names is limited to the locale's default by System.out.println(); replace
  this with an explicit output file if you want Unicode or UTF-8.

  Apache License or GNU General Public License
  --------------------------------------------
  ShowFolderSize1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.
*/

import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors

public class ShowFolderSize1
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL.";
  static final char CSV_COMMA = ','; // separator for comma-separated values
  static final char CSV_QUOTE = '\"'; // quotation for comma-separated values
  static final String DEFAULT_INDENT = "  "; // incremental left indent
  static final int DEFAULT_REPORT = 9; // default subfolder depth to report
  static final int DEFAULT_SEARCH = 99; // default subfolder depth to search
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String PROGRAM_TITLE =
    "Show File Space Used for Folders and Subfolders - by: Keith Fenske";

  /* class variables */

  static boolean caseFlag;        // true if upper/lower case names different
  static boolean csvFlag;         // true for comma-separated values (output)
  static double fixFactor;        // forced scale factor for formatted sizes
  static String fixSuffix;        // forced suffix units for formatted sizes
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatPointOne; // formats with one decimal digit
  static NumberFormat formatPointTwo; // formats with two decimal digits
  static NumberFormat formatUser; // one of the above number formatters
  static String indentString;     // incremental left indent
  static int messageLevel;        // controls the amount of detail to report
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static int reportDepth;         // maximum depth of subfolders to report
  static int searchDepth;         // maximum depth of subfolders to search

/*
  main() method

  We run as a console application.  There is no graphical interface.
*/
  public static void main(String[] args)
  {
    int foldercount;              // number of folders on command line
    Vector folderlist;            // folder names from the command line
    int i;                        // index variable
    String word;                  // one parameter from command line

    /* Initialize global and local variables. */

    caseFlag = false;             // ignore uppercase/lowercase in names
    csvFlag = false;              // default to formatted output, not CSV
    fixFactor = 0.0;              // no forced scale factor for sizes
    fixSuffix = null;             // no forced suffix units for sizes
    folderlist = new Vector();    // no folders found on command line
    indentString = DEFAULT_INDENT; // default incremental left indent
    messageLevel = 1;             // default to report cumulative totals
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    reportDepth = DEFAULT_REPORT; // default subfolder depth to report
    searchDepth = DEFAULT_SEARCH; // default subfolder depth to search

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setGroupingUsed(true); // use commas or digit groups
    formatPointOne.setMaximumFractionDigits(1); // force one decimal digit
    formatPointOne.setMinimumFractionDigits(1);

    formatPointTwo = NumberFormat.getInstance(); // current locale
    formatPointTwo.setGroupingUsed(true); // use commas or digit groups
    formatPointTwo.setMaximumFractionDigits(2); // force two decimal digits
    formatPointTwo.setMinimumFractionDigits(2);

    formatUser = formatPointOne;  // normally format file sizes with this

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a folder name. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      /* Forced scale factors for formatted file sizes (not alphabetical). */

      else if (word.equals("-b") || (mswinFlag && word.equals("/b")))
      {
        fixFactor = 1.0;          // force scale factor for bytes
        fixSuffix = " bytes";     // force suffix units for bytes
        formatUser = formatComma; // no need for decimal digits
      }
      else if (word.equals("-kb") || (mswinFlag && word.equals("/kb")))
      {
        fixFactor = 1024.0;       // force scale factor for kilobytes
        fixSuffix = " KB";        // force suffix units for kilobytes
        formatUser = formatPointOne; // have one decimal digit in sizes
      }
      else if (word.equals("-mb") || (mswinFlag && word.equals("/mb")))
      {
        fixFactor = 1048576.0;    // force scale factor for megabytes
        fixSuffix = " MB";        // force suffix units for megabytes
        formatUser = formatPointOne; // have one decimal digit in sizes
      }
      else if (word.equals("-gb") || (mswinFlag && word.equals("/gb")))
      {
        fixFactor = 1073741824.0; // force scale factor for gigabytes
        fixSuffix = " GB";        // force suffix units for gigabytes
        formatUser = formatPointTwo; // have two decimal digits in sizes
      }
      else if (word.equals("-tb") || (mswinFlag && word.equals("/tb")))
      {
        fixFactor = 1099511627776.0; // force scale factor for terabytes
        fixSuffix = " TB";        // force suffix units for terabytes
        formatUser = formatPointTwo; // have two decimal digits in sizes
      }

      /* Case (lowercase versus uppercase) in file or folder names. */

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        caseFlag = true;          // uppercase/lowercase different in names
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
      {
        caseFlag = false;         // ignore uppercase/lowercase in names
      }

      /* Incremental left indent.  Not everyone wants two spaces! */

      else if (word.startsWith("-i") || (mswinFlag && word.startsWith("/i")))
      {
        /* This option is followed by a non-negative integer for the number of
        spaces in the incremental left indent for showing subfolder levels. */

        int size;                 // temporary variable for user's number
        try { size = Integer.parseInt(word.substring(2)); }
        catch (NumberFormatException nfe) { size = -1; }
        if ((size < 0) || (size > 99))
        {
          System.err.println("Incremental left indent must be from 0 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        char[] spaces = new char[size]; // allocate enough space for spaces
        for (int k = 0; k < size; k ++) spaces[k] = ' '; // fill those spaces
        indentString = new String(spaces); // and convert to a string
      }

      /* Control the amount of detail reported about each folder. */

      else if (word.equals("-m0") || (mswinFlag && word.equals("/m0")))
      {
        messageLevel = 0;         // report details for each folder only
      }
      else if (word.equals("-m1") || (mswinFlag && word.equals("/m1")))
      {
        messageLevel = 1;         // report folder details plus subfolders
      }
      else if (word.equals("-m2") || (mswinFlag && word.equals("/m2")))
      {
        messageLevel = 2;         // permission to be totally excessive
      }

      /* Subfolder report depth and subfolder search depth. */

      else if (word.startsWith("-r") || (mswinFlag && word.startsWith("/r")))
      {
        /* This option is followed by a non-negative integer for the maximum
        depth of subfolders to report. */

        try { reportDepth = Integer.parseInt(word.substring(2)); }
        catch (NumberFormatException nfe) { reportDepth = -1; }
        if ((reportDepth < 0) || (reportDepth > 999))
        {
          System.err.println("Subfolder report depth must be from 0 to 999: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.startsWith("-s") || (mswinFlag && word.startsWith("/s")))
      {
        /* This option is followed by a non-negative integer for the maximum
        depth of subfolders to search. */

        try { searchDepth = Integer.parseInt(word.substring(2)); }
        catch (NumberFormatException nfe) { searchDepth = -1; }
        if ((searchDepth < 0) || (searchDepth > 999))
        {
          System.err.println("Subfolder search depth must be from 0 to 999: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      /* Output as comma-separated values for importing into Microsoft Excel
      and other applications.  Overrides our own formatting options, because
      spreadsheets are easier to format and to change.  This option requires
      some reading of the source code to know what the output values are. */

      else if (word.equals("-v") || (mswinFlag && word.equals("/v"))
        || word.equals("-v1") || (mswinFlag && word.equals("/v1")))
      {
        csvFlag = true;           // output as comma-separated values
      }
      else if (word.equals("-v0") || (mswinFlag && word.equals("/v0")))
      {
        csvFlag = false;          // output with our formatting styles
      }

      /* Anything that looks like an option but which we don't recognize. */

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      /* Parameter does not look like an option.  Assume this is a folder. */

      else
        folderlist.add(args[i]);  // save original folder name for later
    }

    /* All command-line parameters have been successfully parsed. */

    if (folderlist.size() == 0)   // were there any folders found?
    {
      showHelp();                 // show help summary                          // standard code
      System.exit(EXIT_UNKNOWN);  // exit application after printing help       // standard code
//    folderlist.add(".");        // use the current directory instead          // optional code
    }
    if (csvFlag)                  // output as raw comma-separated values?
    {
      /* Column headers (titles) for CSV output.  Must match the printData()
      method.  Comment out lines for fields you don't want.  This text is for
      explaining the data fields.  Users can and should replace the text or
      reformat it for their purposes.  In other words, edit your spreadsheet;
      don't expect this program to do it for you. */

      System.out.println(""
//      + CSV_QUOTE + "Subfolder Depth" + CSV_QUOTE + CSV_COMMA                 // optional code
        + CSV_QUOTE + "Folder Name" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Folder Bytes" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Folder Files" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Folder Subfolders" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Subfolder Bytes" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Subfolder Files" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Subfolder Folders" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Total Bytes" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Total Files" + CSV_QUOTE
        + CSV_COMMA + CSV_QUOTE + "Total Subfolders" + CSV_QUOTE
        );
    }
    foldercount = folderlist.size(); // get the number of folders
    for (i = 0; i < foldercount; i ++) // for each folder found
    {
      String foldername = (String) folderlist.get(i); // get folder name
      ShowFolderSize1Data folderdata = searchFolder(0, new File(foldername));
      if (folderdata == null)     // was folder search successful?
      {
        System.err.println("Folder does not exist: " + foldername);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
      else                        // parameter was a directory (folder)
        printData(0, "", folderdata); // print folder data, no starting indent
    }
    System.exit(EXIT_SUCCESS);

  } // end of main() method


/*
  csvQuotedString() method

  Return a string quoted for importing as a comma-separated value.  The only
  change currently is replacing quotation marks.  Others can be added later.
*/
  static String csvQuotedString(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    buffer.append(CSV_QUOTE);     // starting (opening) quotation mark
    length = input.length();      // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = input.charAt(i);       // get one character from input string
      if (ch == CSV_QUOTE)        // must double any quotation marks
      {
        buffer.append(CSV_QUOTE); // where once was one, now are two
        buffer.append(CSV_QUOTE);
      }
      else
        buffer.append(ch);        // append one character unchanged to result
    }
    buffer.append(CSV_QUOTE);     // ending (closing) quotation mark
    return(buffer.toString());    // give caller our converted string

  } // end of csvQuotedString() method


/*
  formatBytes() method

  Given a number of bytes, return a formatted string with this in kilobytes,
  megabytes, or gigabytes (whichever one is the most expressive).
*/
  static String formatBytes(long size)
  {
    String suffix;                // units string for scaled size
    double units;                 // converted size reduced from bytes

    if (fixFactor > 0.0)          // is there a forced scale factor?
    {
      units = (double) size / fixFactor; // reduce bytes to fixed units
      suffix = fixSuffix;         // matching string with those units
    }
    else                          // find closest scale factor
    {
      units = (double) size / 1024.0; // reduce bytes to kilobytes
      suffix = " KB";             // matching string with those units
      if (units > 999.4) { units = units / 1024.0; suffix = " MB"; }
      if (units > 999.4) { units = units / 1024.0; suffix = " GB"; }
      if (units > 999.4) { units = units / 1024.0; suffix = " TB"; }
    }
    return(formatUser.format(units) + suffix); // scaled into units

  } // end of formatBytes() method


/*
  printData() method

  Give the data object for a folder, print the folder's name, total file size,
  number of files, etc.  Subfolders depend upon <reportDepth>.  We indent each
  additional level by <indentString>.
*/
  static void printData(
    int depth,                    // current subfolder report depth
    String indent,                // current left indent string
    ShowFolderSize1Data givenData) // data object for this folder/subfolder
  {
    int i;                        // index variable
    int newdepth;                 // new report depth for subfolders
    String newindent;             // new left indent for subfolders
    ShowFolderSize1Data[] subfolders; // sorted data for subfolders

    if (csvFlag)                  // output as raw comma-separated values?
    {
      /* Comment out lines for fields you don't want.  If you add formatting
      for numbers, make sure "digit grouping" is turned off -- otherwise you
      may end up with extra commas in the output (American locales).  Either
      that, or turn <CSV_COMMA> into a tab character. */

      System.out.println(""
        + csvQuotedString(indent + givenData.name)                              // standard code
//      + depth                                                                 // optional code
//      + CSV_COMMA + csvQuotedString(givenData.name)                           // optional code
        + CSV_COMMA + givenData.numbyte
        + CSV_COMMA + givenData.numfile
        + CSV_COMMA + givenData.numfold
        + CSV_COMMA + givenData.subbyte
        + CSV_COMMA + givenData.subfile
        + CSV_COMMA + givenData.subfold
        + CSV_COMMA + (givenData.numbyte + givenData.subbyte)
        + CSV_COMMA + (givenData.numfile + givenData.subfile)
        + CSV_COMMA + (givenData.numfold + givenData.subfold)
        );
    }
    else if (givenData.numfold == 0) // are there any subfolders?
    {
      System.out.println(indent + givenData.name + " = "
        + formatBytes(givenData.numbyte) + " in "
        + formatComma.format(givenData.numfile) + " files.");
    }
    else if (messageLevel == 0)   // show details for this folder only
    {
      System.out.println(indent + givenData.name + " = "
        + formatBytes(givenData.numbyte) + " in "
        + formatComma.format(givenData.numfile) + " files with "
        + formatComma.format(givenData.numfold) + " subfolders.");
    }
    else if (messageLevel == 1)   // add subfolder data to folder details
    {
      System.out.println(indent + givenData.name + " = "
        + formatBytes(givenData.numbyte + givenData.subbyte) + " in "
        + formatComma.format(givenData.numfile + givenData.subfile)
        + " files and "
        + formatComma.format(givenData.numfold + givenData.subfold)
        + " subfolders.");
    }
    else                          // assume the "excessive detail" option
    {
      /* Despite the humorous name, this is where you can change the program to
      print any level of detail about folders with subfolders.  Folders without
      subfolders are always handled above, at the beginning. */

      System.out.println(indent + givenData.name + " = "
        + formatBytes(givenData.numbyte) + " in "
        + formatComma.format(givenData.numfile) + " files + "
        + formatBytes(givenData.subbyte) + " in "
        + formatComma.format(givenData.numfold + givenData.subfold)
        + " subfolders.");
    }

    if ((givenData.numfold > 0) && (depth < reportDepth))
                                  // report subfolders until limit reached
    {
      newdepth = depth + 1;       // increase subfolder report depth
      newindent = indent + indentString; // indent each subfolder by this
      subfolders = (ShowFolderSize1Data[]) givenData.sublist.values()
        .toArray(new ShowFolderSize1Data[0]); // no keys, just sorted values
      for (i = 0; i < subfolders.length; i ++) // for each subfolder
        printData(newdepth, newindent, subfolders[i]); // recursively print
    }
  } // end of printData() method


/*
  searchFolder() method

  Given a folder, return a data object with information about that folder.
  Return <null> for errors.  Subfolders depend upon <searchDepth>.
*/
  static ShowFolderSize1Data searchFolder(
    int depth,                    // current subfolder search depth
    File givenFolder)             // folder object to search
  {
    File canon;                   // real (canonical) folder
    File[] contents;              // unsorted contents of folder
    int i;                        // index variable
    int newdepth;                 // new search depth for subfolders
    File next;                    // next File object from <contents>
    ShowFolderSize1Data result;   // our result as a data object
    ShowFolderSize1Data subdata;  // data object for a subfolder

    /* First we need the real (canonical) name of the folder. */

    try { canon = givenFolder.getCanonicalFile(); }
    catch (IOException ioe) { canon = null; }
    if ((canon == null)           // if we can't get real File object
      || (canon.exists() == false) // if File object doesn't exist
      || (canon.isDirectory() == false)) // if it isn't a folder
    {
      return(null);               // no error message, just return nothing
    }

    result = new ShowFolderSize1Data(); // start with empty result
    result.name = canon.getName(); // correct and official folder name
    if ((result.name == null) || (result.name.length() == 0))
      result.name = canon.getPath(); // may be root folder of a drive

    /* We have a real File object that is a directory (folder). */

    contents = canon.listFiles(); // no filter, not sorted
    if (contents == null)         // for protected operating system folders
      contents = new File[0];     // replace with an empty array
    newdepth = depth + 1;         // increase subfolder search depth
    result.sublist = new TreeMap(); // data for a list of subfolders
    for (i = 0; i < contents.length; i ++) // for each file or subfolder
    {
      next = contents[i];         // get next File object from <contents>
      if (next.isDirectory() && (depth < searchDepth)) // is this a subfolder?
      {
        subdata = searchFolder(newdepth, next); // recursively search
        if (subdata != null)      // don't propagate errors
        {
          result.numfold ++;      // one more subfolder in this folder
          result.subbyte += subdata.numbyte + subdata.subbyte;
          result.subfile += subdata.numfile + subdata.subfile;
          result.subfold += subdata.numfold + subdata.subfold;
          result.sublist.put(((caseFlag ? "" : (subdata.name.toLowerCase()
            + " ")) + subdata.name), subdata); // sort by name, save data
        }
      }
      else if (next.isFile())     // is this a normal file?
      {
        result.numbyte += next.length(); // add file size to folder total
        result.numfile ++;        // one more file in this folder
      }
      else { /* silently ignore non-file objects */ }
    }
    return(result);               // give caller whatever we could find

  } // end of searchFolder() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  ShowFolderSize1  [options]  foldernames");      // standard code
//  System.err.println("  java  ShowFolderSize1  [options]  [foldernames]");    // optional code
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -b -kb -mb -gb -tb = show sizes in kilobytes, megabytes, etc.");
    System.err.println("  -c0 = ignore uppercase/lowercase in subfolder names (default)");
    System.err.println("  -c1 = -c = strict Unicode order for case in subfolder names");
    System.err.println("  -i# = incremental left indent for subfolders; default is -i" + DEFAULT_INDENT.length());
    System.err.println("  -m0 = report details for each folder, but don't add subfolders");
    System.err.println("  -m1 = report details for each folder plus subfolders (default)");
    System.err.println("  -m2 = show excessive details about every folder and subfolder");
    System.err.println("  -r# = maximum subfolder depth to report; default is -r" + DEFAULT_REPORT);
    System.err.println("  -s# = maximum subfolder depth to search; default is -s" + DEFAULT_SEARCH);
    System.err.println("  -v0 = output as formatted text with -b or -m options (default)");
    System.err.println("  -v1 = -v = output as raw comma-separated values (see source code)");
    System.err.println();
    System.err.println("Search depth should be large.  Subfolders beyond search depth are ignored (not");
    System.err.println("counted, not reported).  Report depth is less than or equal to search depth.");
//  System.err.println("Default folder is current working directory (\".\")."); // optional code
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method

} // end of ShowFolderSize1 class

// ------------------------------------------------------------------------- //

/*
  ShowFolderSize1Data class

  A data structure to hold information about one folder.
*/

class ShowFolderSize1Data
{
  /* class variables */

  String name;                    // folder name (no path), or null
  long numbyte;                   // number of bytes in this folder only
  long numfile;                   // number of files in this folder only
  long numfold;                   // number of subfolders in this folder only
  long subbyte;                   // number of bytes in all subfolders
  long subfile;                   // number of files in all subfolders
  long subfold;                   // number of subfolders in all subfolders
  TreeMap sublist;                // sorted list of subfolders, or null

  /* constructor (no arguments) */

  public ShowFolderSize1Data()
  {
    this.name = null;             // no folder name
    this.numbyte = 0;             // no bytes in files yet
    this.numfile = 0;             // no files found yet
    this.numfold = 0;             // no subfolders found yet
    this.subbyte = 0;             // no bytes in subfolders yet
    this.subfile = 0;             // no files in subfolders yet
    this.subfold = 0;             // no subfolders in subfolders yet
    this.sublist = null;          // no subfolder list
  }

} // end of ShowFolderSize1Data class

/* Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL. */
