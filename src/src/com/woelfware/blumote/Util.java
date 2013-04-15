// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Util {

	/**
	 * this method will perform x > y with what should be unsigned bytes
	 * @param x
	 * @param y
	 * @return
	 */
    public static boolean isGreaterThanUnsignedByte(int x, int y) {
    	int xl = 0x00FF & x;
    	int yl = 0x00FF & y;
    	
    	if (xl > yl) { return true; }
    	else { return false; }
    }
    
    /**
     * returns integer from two bytes
     * @param upperByte
     * @param lowerByte
     * @return
     */
	public static int bytesToInt(byte upperByte, byte lowerByte) {  
		int i = 0;
	    i |= upperByte & 0xFF;
	    i <<= 8;
	    i |= lowerByte & 0xFF;
	    return i;  
	}	 
	
	public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
	
	
	/** 
	 * adds together two byte arrays
	 * @author keusej
	 *
	 */
	public static byte[] concat(byte[] A, byte[] B) {
		   byte[] C= new byte[A.length+B.length];
		   System.arraycopy(A, 0, C, 0, A.length);
		   System.arraycopy(B, 0, C, A.length, B.length);

		   return C;
	}
	
	/** 
	 * Appends a byte to a byte array
	 * @param A
	 * @param B
	 * @return
	 */
	public static byte[] appendByte(byte[] A, byte B) {
		   byte[] C= new byte[A.length+1];
		   System.arraycopy(A, 0, C, 0, A.length);
		   C[C.length-1] = B;
		   
		   return C;
	}
	
	/** 
	 * Appends a byte array to a byte array.
	 * Returned array is C = B + A, where A is stored in lower index
	 * @param A
	 * @param B
	 * @return
	 */
	public static byte[] appendBytes(byte[] A, byte[] B) {
		   byte[] C= new byte[A.length+B.length];
		   System.arraycopy(A, 0, C, 0, A.length);
		   System.arraycopy(B, 0, C, A.length, B.length);		   
		   return C;
	}
	
	public static String byteArrayToString(byte[] array) {
		Formatter formatter = new Formatter();
		for (byte b : array) {
			formatter.format("%02x", b);
		}
		String returnString = formatter.toString();
		formatter.close();
		return returnString;
	}
	
	public static String oneHexByteToString(byte b) {
		Formatter formatter = new Formatter();		
		formatter.format("%02x", b);
		String returnString = formatter.toString();
		formatter.close();
		return returnString;		
	}	

	
	public static class FileUtils {
		/**
		 * Create a copy of of a file
		 * @param fromFile file to copy from
		 * @param toFile file to copy to
		 */
		public static void copyFile(File fromFile, File toFile) throws IOException {
			FileChannel fromChannel = null;
			FileChannel toChannel = null;
			try {
				fromChannel = new FileInputStream(fromFile).getChannel();
				toChannel = new FileOutputStream(toFile).getChannel();
				fromChannel.transferTo(0, fromChannel.size(), toChannel);
			} finally {
				try {
					if (fromChannel != null) {
						fromChannel.close();
					}
				} finally {
					if (toChannel != null) {
						toChannel.close();
					}
				}
			}
		}
		
		public static void insertLine(File filename, String line, int linenum) throws IOException {			
			// temp file
			File outFile = new File(filename.getCanonicalPath()+".bak");

			// input
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));
			// output
			FileOutputStream fos = new FileOutputStream(outFile);
			PrintWriter out = new PrintWriter(fos);
			String thisLine = "";
			int i = 1;
			while ((thisLine = in.readLine()) != null) {
				if (i == linenum)
					out.println(line);
				out.println(thisLine);
				i++;
			}
			out.flush();
			out.close();
			in.close();

			filename.delete();
			outFile.renameTo(filename);	  
		}
		
		/**
		 * checks the md5sum for a given file.  Compares to md5 parameter string.
		 * @param md5
		 * @param fileDir
		 * @return
		 */
		public static boolean checkMD5(String md5, File fileDir, String fileName) {
			if (md5 == null || md5 == "" || fileDir == null || fileName == "") {
				return false;
			}
			String calculatedDigest = calculateMD5(fileDir, fileName);
			if (calculatedDigest == null) {
				return false;
			}
			return calculatedDigest.equalsIgnoreCase(md5);
		}

		public static String calculateMD5(File fileDir, String fileName) {
			MessageDigest digest = null;
			try {
				digest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
			
			InputStream is = null;
			try {
				is = new FileInputStream(new File(fileDir, fileName));
			} catch (FileNotFoundException e) {
				return null;
			}
			
			byte[] buffer = new byte[8192];
			int read = 0;
			try {
				while ((read = is.read(buffer)) > 0) {
					digest.update(buffer, 0, read);
				}
				byte[] md5sum = digest.digest();
				BigInteger bigInt = new BigInteger(1, md5sum);
				String output = bigInt.toString(16); // create hex representation as a string
				// Fill to 32 chars
				output = String.format("%32s", output).replace(' ', '0');
				return output;
			} catch (IOException e) {
				throw new RuntimeException("Unable to process file for MD5", e);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					throw new RuntimeException(
							"Unable to close input stream for MD5 calculation", e);
				}
			}
		}
	}
}
