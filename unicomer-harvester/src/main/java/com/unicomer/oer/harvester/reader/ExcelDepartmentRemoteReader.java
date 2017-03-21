/**
 * 
 */
package com.unicomer.oer.harvester.reader;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.flashline.registry.openapi.entity.AuthToken;
import com.flashline.registry.openapi.service.v300.FlashlineRegistryTr;
import com.flashline.registry.openapi.service.v300.FlashlineRegistryTrServiceLocator;

/**
 * @author carlosj_rodriguez
 *
 */
public class ExcelDepartmentRemoteReader {
	static String uri = "http://uoerepap01tst.unicomer.com:8111/oer/services/FlashlineRegistryTr";
	static String username = "carlos_jimenez";
	static String password = "oer1234";
	
	public static void main(String[] args) {
		try {
			FileInputStream file = new FileInputStream(new File("C:\\Users\\carlosj_rodriguez\\Drive\\unicomer-docs\\architecture\\04 - Service repository OER\\Departamentos Unicomer.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			FlashlineRegistryTr registry = new FlashlineRegistryTrServiceLocator().getFlashlineRegistryTr(new URL(uri));
			AuthToken token = registry.authTokenCreate(username, password);
			
			Iterator<Row> rowIterator = sheet.iterator();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				Iterator<Cell> cellIterator = row.cellIterator();
				String name = cellIterator.next().getStringCellValue();
				String description = cellIterator.next().getStringCellValue();
				System.out.println("Creando departamento " + name);
				registry.departmentCreate(token, name, description);
			}
			workbook.close();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
