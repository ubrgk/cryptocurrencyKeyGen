package com.ubrgk.crypto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 *
 */
@SuppressFBWarnings("DM_EXIT")
class Guide {
    private static final String CSV_EXTENTION = ".csv";
    private static final String LABEL_PUBLIC_ADDRESSES_ONLY = "-public_addresses_only";

    /**
     * Print a summary of the license and a warning about the dangers of managing
     * manually managing cryptocurrency keys.
     *
     * Suppress DM_EXIT because this application is intended to be executed alone.
     */
    void doDisclaimer(final Scanner sc) {
        println("------------------------------------ LICENSE ------------------------------------");
        println("| THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    |");
        println("| IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      |");
        println("| FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   |");
        println("| AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        |");
        println("| LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, |");
        println("| OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE |");
        println("| SOFTWARE.                                                                     |");
        println("---------------------------------------------------------------------------------");
        println("");
        println("       --------------------------*** WARNING ***--------------------------");
        println("       | This software produces unencrypted cryptocurrency private keys. |");
        println("       | Knowledge of a private key associated to a balance grants the   |");
        println("       | ability to create transactions using that balance.              |");
        println("       |                                                                 |");
        println("       | DO NOT send a balance to the keys generated by this software    |");
        println("       | if you do not understand the risks.                             |");
        println("       -------------------------------------------------------------------");
        println("");
        askForInput("Ready to proceed? (y/N)");
        final String answer = sc.next();
        if (!"y".equals(answer)) {
            println("Aborted.");
            System.exit(0);
        }
    }

    CryptoCurrencyType doSelectCryptoType(final Scanner sc) {
        final CryptoCurrencyType[] values = CryptoCurrencyType.values();
        CryptoCurrencyType value = null;
        while (value == null) {
            println("Supported crypto-currency types:");
            for (int i = 0; i < values.length; i++) {
                println(" " + i + ". " + values[i].getAbbreviation());
            }
            askForInput("Which crypto?");

            int menuSelection;
            try {
                menuSelection = sc.nextInt();
            } catch (InputMismatchException e) {
                println(getInvalidSelectionResponse("non-numeric"));
                sc.next();
                continue;
            }

            try {
                value = values[menuSelection];
                println("Selected " + value);
            } catch (ArrayIndexOutOfBoundsException e) {
                println(getInvalidSelectionResponse(String.valueOf(menuSelection)));
            }
        }
        return value;
    }

    String doGenerateFiles(final Scanner sc, final CryptoCurrencyType crypto) throws IOException {
        /*
         * Generate content
         */

        Integer addressCount = null;
        while (addressCount == null) {
            askForInput("How many addresses to generate?");
            try {
                addressCount = sc.nextInt();
            } catch (InputMismatchException e) {
                println(getInvalidSelectionResponse("non-numeric"));
                sc.next();
            }
        }

        println("Creating file content...");
        final List<ECKey> ecKeys = KeyGenerator.generateKeys(addressCount);
        println("... generated " + ecKeys.size() + " addresses.");

        final List<List<String>> rows = CsvBuilder.createLinesOfFields(ecKeys, crypto);
        final String content = CsvBuilder.toCsv(rows);

        /*
         * Save file
         */

        askForInput("Name for file (e.g. \"my-keys\")?");
        String userFilePath = sc.next();
        final String filePath = userFilePath + CSV_EXTENTION;
        writeContentToFile(sc, content, filePath);

        /*
         * Verify file
         */

        println("Reading file for verification...");
        final Path path = Paths.get(filePath);
        final List<String> lines = Files.readAllLines(path);
        try {
            CsvBuilder.verify(lines, crypto.getNetParams());
        } catch (AssertionError e) {
            Files.move(path, path.resolveSibling(filePath + "-failed_verification"));
            System.exit(0);
        }

        /*
         * Create public addresses file
         */

        final List<List<String>> onlyPubFieldsRows = CsvBuilder.getOnlyPubFields(rows);
        final String onlyPubContent = CsvBuilder.toCsv(onlyPubFieldsRows);
        final String onlyPubFilePath = userFilePath + LABEL_PUBLIC_ADDRESSES_ONLY + CSV_EXTENTION;
        writeContentToFile(sc, onlyPubContent, onlyPubFilePath);

        return filePath;
    }

    private void writeContentToFile(final Scanner sc, final String fullContent, final String filePath) throws IOException {
        final Path path = Paths.get(filePath);
        println("Saving file " + path + "...");
        try {
            Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            print("File already exists, overwrite? (y/N) ");
            final String overwriteAnswer = sc.next();
            if ("y".equals(overwriteAnswer)) {
                println("Overwriting file...");
            } else {
                println("Aborted.");
                System.exit(0);
            }
        }
        final byte[] strToBytes = fullContent.getBytes(StandardCharsets.UTF_8);
        Files.write(path, strToBytes);
        println("... saved.");
    }

    void doEncryptionExample(final String filePath) {
        println("");
        println("The following is an encryption example WITHOUT ANY WARRANTY, that uses");
        println("an implementation of OpenPGP to encrypt the generated file.");
        println("Encrypt:");
        println("  gpg --armor --symmetric --cipher-algo AES256 --s2k-digest-algo SHA512 --s2k-count 65011712 " + filePath);
        println("Decrypt:");
        println("  gpg -d " + filePath);
    }

    private String getInvalidSelectionResponse(final String menuOptionNumber) {
        return "Selected " + menuOptionNumber + "; invalid selection.";
    }

    private void askForInput(final String s) {
        System.out.print(s + " ");
    }

    private void println(final String s) {
        System.out.println(s);
    }

    private void print(final String s) {
        System.out.print(s);
    }
}
