package pt.promatik.moss.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import pt.promatik.moss.utils.Utils;

public class FileLogger 
{
	private String path = "";
	private String filename = "";
	private String extension = "log";

	public FileLogger() { }

	public void init(String path, String filename)
	{
		this.path = path;
		this.filename = filename;
	}

	public void add(String file_suffix, String message)
	{
		if(path.equals("")) {
			Utils.error("FileLogger is not initialized, run filelog.init()");
		} else {
			try{
				FileWriter fw = new FileWriter(String.format("%s%s_%s.%s", path, filename, file_suffix, extension), true);
				fw.write(Calendar.getInstance().getTime() + ": " + message + "\r\n");
				fw.flush();
				fw.close();
			} catch(IOException e) {
				Utils.log(e);
			}
		}
	}
}