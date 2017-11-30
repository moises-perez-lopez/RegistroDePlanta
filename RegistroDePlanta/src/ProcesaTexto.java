import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.IS2.Barracus.ChapuzaProcesado;
import org.IS2.Nikola.ExcepcionEnTrabajo;
import org.IS2.Nikola.UtilidadesTexto;
import org.IS2.Tesla.IS2_Tesla_raw_task_thrd;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ProcesaTexto {
	private static final String FICHERO_ENTRADA = "fichero.entrada";
	private static final String CARPETA_ENTRADA = "carpeta.entrada";
	private static final String FICHERO_SALIDA = "fichero.salida";
	private static final String CARPETA_SALIDA = "carpeta.salida";
	private Properties propiedades;
	private File ficheroSalida;
	private ArrayList<File> listaFicherosAProcesar;
	private JPanel panel = new JPanel();
	TreeMap<String, Integer> mapaCabecera;
	ArrayList<TreeMap<String, String>> listaMapasParametroValorPorRegistro;
	ArrayList<TreeMap<String, LinkedHashMap<String,String>>> listaMapasParametroValorPorRegistro4G;
	
	public ProcesaTexto(String sConfiguracion) throws ExcepcionEnTrabajo {
		leePropiedades(sConfiguracion);
		System.out.println("Instanciado servicio de descarga con trabajo: " + sConfiguracion);
		start();
	}

	private void leePropiedades(String sConfiguracion) throws ExcepcionEnTrabajo {
		System.out.println("Leemos configuracion...");
		File ficheroConfiguracion = new File(sConfiguracion);
		if (!ficheroConfiguracion.exists()) {
			ficheroConfiguracion = new File(sConfiguracion + ".txt");
		}

		if (!ficheroConfiguracion.exists()) {
			JOptionPane.showMessageDialog(panel, "No existe fichero de configuración: " + sConfiguracion, "Error", JOptionPane.ERROR_MESSAGE);
			throw new ExcepcionEnTrabajo();
		}

		propiedades = new Properties();

		try (FileInputStream in = new FileInputStream(ficheroConfiguracion)) {
			propiedades.load(in);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panel, "Error en la lectura del fichero de configuracion: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}

		System.out.println("Configuracion leida.");
	}

	private void start() throws ExcepcionEnTrabajo {

		compruebaConfiguracion();

		if (todosLosFicherosDisponibles()) {
				procesoFicheros();
		}
		System.out.println("FIN");
		
	}

	private void compruebaConfiguracion() throws ExcepcionEnTrabajo {

		System.out.println("Comprobamos configuracion...");

		String[] asPropiedadesNecesarias = { FICHERO_ENTRADA, CARPETA_ENTRADA, FICHERO_SALIDA,CARPETA_SALIDA};

		for (String sPropiedadNecesaria : asPropiedadesNecesarias) {
			if (!propiedades.containsKey(sPropiedadNecesaria)) {
				JOptionPane.showMessageDialog(panel,"Falta parametro '" + sPropiedadNecesaria + "' en el fichero de configuración.", "Error", JOptionPane.ERROR_MESSAGE);
				throw new ExcepcionEnTrabajo();
				
			}
		}

		System.out.println("Configuracion correcta.");

	}

	private boolean todosLosFicherosDisponibles() throws ExcepcionEnTrabajo {
		String sCarpetaOrigen = propiedades.getProperty(CARPETA_ENTRADA);
		String sListaFicherosNecesarios = propiedades.getProperty(FICHERO_ENTRADA);
		String[] aListaFicherosNecesarios = UtilidadesTexto.divideTextoEnTokens(sListaFicherosNecesarios, ",");
		listaFicherosAProcesar = new ArrayList<File>();
		File carpetaFuenteDatos = new File(sCarpetaOrigen.trim());
		if (!carpetaFuenteDatos.exists()) {
			JOptionPane.showMessageDialog(panel, "Aun falta la carpeta " + carpetaFuenteDatos.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		for (String sNombreFicheroOrigen : aListaFicherosNecesarios) {
			sNombreFicheroOrigen = sNombreFicheroOrigen.trim();

			File ficheroEntrada = new File(new File(sCarpetaOrigen), sNombreFicheroOrigen);
			if(ficheroEntrada.getName().contains("2G")){
				if(ficheroEntrada.length()>20000000){
					JOptionPane.showMessageDialog(panel, "El fichero " + ficheroEntrada.getName() + " tiene un tamaño mayor de lo habitual, comprobar si es correcto.", "", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			if(ficheroEntrada.getName().contains("3G")){
				if(ficheroEntrada.length()>55000000){
					JOptionPane.showMessageDialog(panel, "El fichero " + ficheroEntrada.getName() + " tiene un tamaño mayor de lo habitual, comprobar si es correcto.", "", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			if(ficheroEntrada.getName().contains("4G")){
				if(ficheroEntrada.length()>14000000){
					JOptionPane.showMessageDialog(panel, "El fichero " + ficheroEntrada.getName() + " tiene un tamaño mayor de lo habitual, comprobar si es correcto.", "", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			listaFicherosAProcesar.add(ficheroEntrada);
		}

		System.out.println("Parece que ya tenemos todos los ficheros disponibles");

		return true;
	}

	private void procesoFicheros() throws ExcepcionEnTrabajo {

		System.out.println("Iniciamos analisis de ficheros");
		mapaCabecera = new TreeMap<String, Integer>();
		System.out.println("hay que procesar : " + listaFicherosAProcesar.size());
		for (File unFicheroAProcesar : listaFicherosAProcesar) {
			System.out.println("Analizamos fichero " + unFicheroAProcesar.getAbsolutePath());
			if (unFicheroAProcesar.getName().contains("2G")) {
				leerFichero2G3G(unFicheroAProcesar,"2G");
				escribirFichero2G(listaMapasParametroValorPorRegistro);
			}
			if (unFicheroAProcesar.getName().contains("3G")) {
				leerFichero2G3G(unFicheroAProcesar,"3G");
				escribirFichero3G(listaMapasParametroValorPorRegistro);
			}
			if (unFicheroAProcesar.getName().contains("4G")) {
				leerFichero4G(unFicheroAProcesar);
				escribirFichero4G();
			}
		}

	}

	private boolean escribirFichero2G(ArrayList<TreeMap<String, String>> listaMapasParametroValorPorRegistro) {

		ArrayList<String> listaParametrosCabecera = completarListaParametros();

		String sFicheroSalida = propiedades.getProperty(FICHERO_SALIDA);
		String sCarpetaSalida = propiedades.getProperty(CARPETA_SALIDA);
		ficheroSalida = new File(new File(sCarpetaSalida), sFicheroSalida);
		String [] sNombreFichero = UtilidadesTexto.divideTextoEnTokens(ficheroSalida.getName(),".");
		if((sNombreFichero==null)||(sNombreFichero.length!=2)){
			JOptionPane.showMessageDialog(panel, "Hay un error con el fichero de salida en el fichero de configuración.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}else{
			if(ficheroSalida.getName().contains("2G")){
				if(!sNombreFichero[1].equals("cel")){
					JOptionPane.showMessageDialog(panel, "La extensión del fichero 2G debe ser .cel ", "Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}
			
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida))) {
			bw.write("55 TEMS_-_Cell_names \n");
			for (String sParametro : listaParametrosCabecera) {
				bw.write(sParametro + "\t");
			}
			bw.write("\n");
			for (TreeMap<String, String> mapaParametroValor : listaMapasParametroValorPorRegistro) {
				
				for (String sParametro : listaParametrosCabecera) {
					if (mapaParametroValor.containsKey(sParametro)) {
						bw.write(mapaParametroValor.get(sParametro) + "\t");
					} else {
						bw.write("\t");
					}
				}
				bw.write("\n");

			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panel, "Hay un problema con el fichero de salida.", "", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;

	}
	
	private boolean escribirFichero3G(ArrayList<TreeMap<String, String>> listaMapasParametroValorPorRegistro) {

		ArrayList<String> listaParametrosCabecera = completarListaParametros();
		listaParametrosCabecera.add(21,"CELL_TYPE");
		String sFicheroSalida = propiedades.getProperty(FICHERO_SALIDA);
		String sCarpetaSalida = propiedades.getProperty(CARPETA_SALIDA);
		ficheroSalida = new File(new File(sCarpetaSalida), sFicheroSalida);
		String [] sNombreFichero = UtilidadesTexto.divideTextoEnTokens(ficheroSalida.getName(),".");
		
		if((sFicheroSalida==null)||(sNombreFichero.length!=2)){
			JOptionPane.showMessageDialog(panel, "Hay un error con el fichero de salida en el fichero de configuración.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}else{
			if(ficheroSalida.getName().contains("3G")){
				if(!sNombreFichero[1].equals("cel")){
					JOptionPane.showMessageDialog(panel, "La extensión del fichero 3G debe ser .cel ", "Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida))) {
			bw.write("55 TEMS_-_Cell_names \n");
			for (String sParametro : listaParametrosCabecera) {
				bw.write(sParametro + "\t");
			}
			bw.write("\n");
			for (TreeMap<String, String> mapaParametroValor : listaMapasParametroValorPorRegistro) {
				
				for (String sParametro : listaParametrosCabecera) {
					if (mapaParametroValor.containsKey(sParametro)) {
						bw.write(mapaParametroValor.get(sParametro) + "\t");
					} else {
						bw.write("\t");
					}
				}
				bw.write("\n");

			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panel, "Hay un problema con el fichero de salida.", "", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	private boolean escribirFichero4G() {

		ArrayList<String> listaParametrosCabecera = completarListaParametros4G();
		
		String sFicheroSalida = propiedades.getProperty(FICHERO_SALIDA);
		String sCarpetaSalida = propiedades.getProperty(CARPETA_SALIDA);
		ficheroSalida = new File(new File(sCarpetaSalida), sFicheroSalida);
		
		String [] sNombreFichero = UtilidadesTexto.divideTextoEnTokens(ficheroSalida.getName(),".");
		if((sNombreFichero==null)||(sNombreFichero.length!=2)){
			JOptionPane.showMessageDialog(panel, "Hay un error con el fichero de salida en el fichero de configuración.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}else{
			if(ficheroSalida.getName().contains("4G")){
				if(!sNombreFichero[1].equals("xml")){
					JOptionPane.showMessageDialog(panel, "La extensión del fichero 4G debe ser .xml ", "Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida))) {
			bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			bw.write("<TEMS_CELL_EXPORT VERSION=\"1.0\" xmlns:dataType=\"http://www.ericsson.com/tems/dataTypes\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"TEMSCell.xsd\">\r\n");
			bw.write("<GENERATED_DATE>2009-01-06</GENERATED_DATE>\r\n");
			bw.write("<LTE VERSION=\"1.0\">\r\n");
			bw.write("<CELL_LIST NET_OPERATOR=\"Ericsson\">\r\n");
		
			for (TreeMap<String, LinkedHashMap<String,String>> mapaRaizParametroValor : listaMapasParametroValorPorRegistro4G) {
				bw.write("<LTE_CELL>\r\n");
				for (String sParametro : listaParametrosCabecera) {
					if (mapaRaizParametroValor.containsKey(sParametro)) {
						
						if((!sParametro.equalsIgnoreCase("LTE_BANDWIDTH"))&&(!sParametro.equalsIgnoreCase("POSITION"))&&(!sParametro.equalsIgnoreCase("NEIGHBOR_LIST"))&&(!sParametro.equalsIgnoreCase("LTE_CGI"))&&(!sParametro.equalsIgnoreCase("ANTENNA"))){
							bw.write("<"+sParametro+">"+mapaRaizParametroValor.get(sParametro).get(sParametro) + "</"+sParametro+">\r\n");
						}
						if(sParametro.equalsIgnoreCase("LTE_BANDWIDTH")){
							bw.write("<LTE_BANDWIDTH></LTE_BANDWIDTH>\r\n");
						}
						if(sParametro.equalsIgnoreCase("POSITION")){
							bw.write("<POSITION>\r\n");
							for(String sParametroAux : mapaRaizParametroValor.get(sParametro).keySet()){
								bw.write("<"+sParametroAux+">"+mapaRaizParametroValor.get(sParametro).get(sParametroAux) + "</"+sParametroAux+">\r\n");
							}
							bw.write("</POSITION>\r\n");
						}
						if(sParametro.equalsIgnoreCase("NEIGHBOR_LIST")){
							bw.write("<NEIGHBOR_LIST>\r\n");
							for(String sParametroAux : mapaRaizParametroValor.get(sParametro).keySet()){
								bw.write("<"+sParametroAux+">"+mapaRaizParametroValor.get(sParametro).get(sParametroAux) + "</"+sParametroAux+">\r\n");
							}
							bw.write("</NEIGHBOR_LIST>\r\n");
						}
						if(sParametro.equalsIgnoreCase("LTE_CGI")){
							bw.write("<LTE_CGI>\r\n");
							for(String sParametroAux : mapaRaizParametroValor.get(sParametro).keySet()){
								bw.write("<"+sParametroAux+">"+mapaRaizParametroValor.get(sParametro).get(sParametroAux) + "</"+sParametroAux+">\r\n");
							}
							bw.write("</LTE_CGI>\r\n");
						}
						if(sParametro.equalsIgnoreCase("ANTENNA")){
							bw.write("<ANTENNA>\r\n");
							for(String sParametroAux : mapaRaizParametroValor.get(sParametro).keySet()){
								bw.write("<"+sParametroAux+">"+mapaRaizParametroValor.get(sParametro).get(sParametroAux) + "</"+sParametroAux+">\r\n");
							}
							bw.write("</ANTENNA>\r\n");
						}
					} else {
						bw.write("\t");
					}
				}
				bw.write("</LTE_CELL>\r\n");
				
			}
			bw.write("</CELL_LIST>\r\n");
			bw.write("</LTE>\r\n");
			bw.write("</TEMS_CELL_EXPORT>\r\n");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panel, "Hay un problema con el fichero de salida.", "", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;

	}

	private ArrayList<String> completarListaParametros4G() {
		ArrayList<String> listaParametros = new ArrayList<String>();
		listaParametros.add("CELLNAME");
		listaParametros.add("EARFCN_DL");
		listaParametros.add("POSITION");
		listaParametros.add("RS_POWER");
		listaParametros.add("ENODE_B");
		listaParametros.add("ENODE_B_STATUS");
		listaParametros.add("LTE_BANDWIDTH");
		listaParametros.add("NEIGHBOR_LIST");
		listaParametros.add("PHYSICAL_LAYER_CELL_ID");
		listaParametros.add("LTE_CGI");
		listaParametros.add("ANTENNA");
					
		return listaParametros;
	}

	private ArrayList<String> completarListaParametros() {
		ArrayList<String> listaParametros = new ArrayList<String>();
		listaParametros.add("Cell");
		listaParametros.add("UARFCN");
		listaParametros.add("SC");
		listaParametros.add("ARFCN");
		listaParametros.add("BSIC");
		listaParametros.add("Lat");
		listaParametros.add("Lon");
		listaParametros.add("MCC");
		listaParametros.add("MNC");
		listaParametros.add("LAC");
		listaParametros.add("CI");
		listaParametros.add("RA");
		listaParametros.add("URA");
		listaParametros.add("TIME_OFFSET");
		listaParametros.add("CPICH_POWER");
		listaParametros.add("MAX_TX_POWER");
		listaParametros.add("ANT_ORIENTATION");
		listaParametros.add("ANT_BEAM_WIDTH");
		listaParametros.add("ANT_TYPE");
		listaParametros.add("ANT_HEIGHT");
		listaParametros.add("ANT_TILT");
		listaParametros.add("NODE_B");
		listaParametros.add("NODE_B_STATUS");
		listaParametros.add("CI_N_1");
		listaParametros.add("CI_N_2");
		listaParametros.add("CI_N_3");
		listaParametros.add("CI_N_4");
		listaParametros.add("CI_N_5");
		listaParametros.add("CI_N_6");
		listaParametros.add("CI_N_7");
		listaParametros.add("CI_N_8");
		listaParametros.add("CI_N_9");
		listaParametros.add("CI_N_10");
		listaParametros.add("CI_N_11");
		listaParametros.add("CI_N_12");
		listaParametros.add("CI_N_13");
		listaParametros.add("CI_N_14");
		listaParametros.add("CI_N_15");
		listaParametros.add("CI_N_16");
		listaParametros.add("CI_N_17");
		listaParametros.add("CI_N_18");
		listaParametros.add("CI_N_19");
		listaParametros.add("CI_N_20");
		listaParametros.add("CI_N_21");
		listaParametros.add("CI_N_22");
		listaParametros.add("CI_N_23");
		listaParametros.add("CI_N_24");
		listaParametros.add("CI_N_25");
		listaParametros.add("CI_N_26");
		listaParametros.add("CI_N_27");
		listaParametros.add("CI_N_28");
		listaParametros.add("CI_N_29");
		listaParametros.add("CI_N_30");
		listaParametros.add("CI_N_31");
		listaParametros.add("CI_N_32");

		return listaParametros;

	}

	private void leerFichero2G3G(File unFicheroAProcesar,String sTecnologia) {
		listaMapasParametroValorPorRegistro = new ArrayList<TreeMap<String, String>>();
		TreeMap<String, String> mapaParametroValor;

		try (FileReader fr = new FileReader(unFicheroAProcesar); BufferedReader br = new BufferedReader(fr)) {
			String sCabeceraFichero = br.readLine();
			if (sCabeceraFichero == null) {
				System.out.println("No hay cabecera en el fichero " + unFicheroAProcesar.getName());
			}
			retornaMapaCabecera(sCabeceraFichero);
			
			String sValoresParametros = br.readLine();
			
			while (sValoresParametros != null) {

				String[] aValoresParametros = UtilidadesTexto.divideTextoEnTokens(sValoresParametros, "\t");
				if (aValoresParametros != null) {
					if (aValoresParametros[mapaCabecera.get("Supplier")].equalsIgnoreCase("ericsson")) {
						mapaParametroValor = new TreeMap<String, String>();
						String sSiteId = aValoresParametros[mapaCabecera.get("SiteID")];
						String sCell = aValoresParametros[mapaCabecera.get("Cell")];
						String sLongitud = aValoresParametros[mapaCabecera.get("Longitude")];
						String sLatitud = aValoresParametros[mapaCabecera.get("Latitude")];
						String sCadenaUMT2Grados = "30 T "+sLongitud+" "+sLatitud;
						ArrayList<Double> listaLongitudLatitud = convierteUTM2Grados(sCadenaUMT2Grados);
						
						if ((sSiteId != null) && (sSiteId.length() > 0) && (sCell != null) && (sCell.length() > 0)) {
							String sCellFinal = sSiteId + "_" + sCell.substring(sCell.length() - 1);
							mapaParametroValor.put("Cell", sCellFinal);
							mapaParametroValor.put("Supplier", aValoresParametros[mapaCabecera.get("Supplier")]);
							if(sTecnologia.equalsIgnoreCase("2G")){
								mapaParametroValor.put("ARFCN", aValoresParametros[mapaCabecera.get("BCCH")]);
							}
							if(sTecnologia.equalsIgnoreCase("3G")){
								mapaParametroValor.put("UARFCN", aValoresParametros[mapaCabecera.get("UARFCN")]);
								mapaParametroValor.put("SC", aValoresParametros[mapaCabecera.get("SC")]);
							}
							if(sTecnologia.equalsIgnoreCase("2G")){
								mapaParametroValor.put("BSIC", aValoresParametros[mapaCabecera.get("BSIC")]);
							}
							if ((listaLongitudLatitud!=null)&&(listaLongitudLatitud.size()==2)){
								mapaParametroValor.put("Lon", String.valueOf(listaLongitudLatitud.get(0)));
								mapaParametroValor.put("Lat", String.valueOf(listaLongitudLatitud.get(1)));
							}
							mapaParametroValor.put("MCC", aValoresParametros[mapaCabecera.get("MCC")]);
							mapaParametroValor.put("MNC", aValoresParametros[mapaCabecera.get("MNC")]);
							mapaParametroValor.put("LAC", aValoresParametros[mapaCabecera.get("LAC")]);
							if(sTecnologia.equalsIgnoreCase("2G")){
								mapaParametroValor.put("CI", aValoresParametros[mapaCabecera.get("CI")]);
							}
							if(sTecnologia.equalsIgnoreCase("3G")){
								mapaParametroValor.put("CI", aValoresParametros[mapaCabecera.get("WCDMA_CI")]);
							}
							mapaParametroValor.put("ANT_ORIENTATION", aValoresParametros[mapaCabecera.get("Azimuth")]);
							mapaParametroValor.put("ANT_BEAM_WIDTH", aValoresParametros[mapaCabecera.get("Beamwidth")]);
							listaMapasParametroValorPorRegistro.add(mapaParametroValor);
						}
					}
					
				}
				
				sValoresParametros = br.readLine();
			}
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(panel, "Hay un problema con el fichero de entrada.", "", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	
	private void leerFichero4G(File unFicheroAProcesar) {
		listaMapasParametroValorPorRegistro4G = new ArrayList<TreeMap<String, LinkedHashMap<String,String>>>();
		TreeMap<String, LinkedHashMap<String,String>> mapaParametroValor;

		try (FileReader fr = new FileReader(unFicheroAProcesar); BufferedReader br = new BufferedReader(fr)) {
			String sCabeceraFichero = br.readLine();
			if (sCabeceraFichero == null) {
				System.out.println("No hay cabecera en el fichero " + unFicheroAProcesar.getName());
			}
			retornaMapaCabecera(sCabeceraFichero);
			
			String sValoresParametros = br.readLine();
			
			while (sValoresParametros != null) {

				String[] aValoresParametros = UtilidadesTexto.divideTextoEnTokens(sValoresParametros, "\t");
				if (aValoresParametros != null) {
					if (aValoresParametros[mapaCabecera.get("Supplier")].equalsIgnoreCase("ericsson")) {
						
						mapaParametroValor = new TreeMap<String, LinkedHashMap<String,String>>();

						if(!mapaParametroValor.containsKey("CELLNAME")){				
							mapaParametroValor.put("CELLNAME", new LinkedHashMap<String,String>());
							mapaParametroValor.get("CELLNAME").put("CELLNAME", convierteCellName(aValoresParametros[mapaCabecera.get("SectorID")]));
						}
						if(!mapaParametroValor.containsKey("EARFCN_DL")){
							mapaParametroValor.put("EARFCN_DL", new LinkedHashMap<String,String>());
							mapaParametroValor.get("EARFCN_DL").put("EARFCN_DL",aValoresParametros[mapaCabecera.get("DL_EARFCN")]);
						}
						if(!mapaParametroValor.containsKey("POSITION")){
							mapaParametroValor.put("POSITION", new LinkedHashMap<String,String>());
							mapaParametroValor.get("POSITION").put("GEODETIC_DATUM", "WGS84");
						}
						
						String sLongitud = aValoresParametros[mapaCabecera.get("Longitude")];
						String sLatitud = aValoresParametros[mapaCabecera.get("Latitude")];
						String sCadenaUMT2Grados = "30 T "+sLongitud+" "+sLatitud;
						ArrayList<Double> listaLongitudLatitud = convierteUTM2Grados(sCadenaUMT2Grados);				
						if ((listaLongitudLatitud!=null)&&(listaLongitudLatitud.size()==2)){
							if(!mapaParametroValor.get("POSITION").containsKey("LATITUDE")){
								mapaParametroValor.get("POSITION").put("LATITUDE", String.valueOf(listaLongitudLatitud.get(1)));
								
							}
							if(!mapaParametroValor.get("POSITION").containsKey("LONGITUDE")){
								mapaParametroValor.get("POSITION").put("LONGITUDE", String.valueOf(listaLongitudLatitud.get(0)));
							}
						}
						if(!mapaParametroValor.containsKey("RS_POWER")){
							mapaParametroValor.put("RS_POWER", new LinkedHashMap<String,String>());
							mapaParametroValor.get("RS_POWER").put("RS_POWER", "-94.6");
						}
						
						if(!mapaParametroValor.containsKey("ENODE_B")){
							mapaParametroValor.put("ENODE_B", new LinkedHashMap<String,String>());
							mapaParametroValor.get("ENODE_B").put("ENODE_B", aValoresParametros[mapaCabecera.get("eNodeBName")]);
						}
						
						if(!mapaParametroValor.containsKey("ENODE_B_STATUS")){
							mapaParametroValor.put("ENODE_B_STATUS", new LinkedHashMap<String,String>());
							mapaParametroValor.get("ENODE_B_STATUS").put("ENODE_B_STATUS", "");
						}
						
						if(!mapaParametroValor.containsKey("LTE_BANDWIDTH")){
							mapaParametroValor.put("LTE_BANDWIDTH", new LinkedHashMap<String,String>());
							mapaParametroValor.get("LTE_BANDWIDTH").put("LTE_BANDWIDTH", "????");
						}
						
						if(!mapaParametroValor.containsKey("NEIGHBOR_LIST")){
							mapaParametroValor.put("NEIGHBOR_LIST", new LinkedHashMap<String,String>());
							mapaParametroValor.get("NEIGHBOR_LIST").put("CELLNAME", aValoresParametros[mapaCabecera.get("SectorID")]);
						}
						
						if(!mapaParametroValor.containsKey("PHYSICAL_LAYER_CELL_ID")){
							mapaParametroValor.put("PHYSICAL_LAYER_CELL_ID", new LinkedHashMap<String,String>());
							mapaParametroValor.get("PHYSICAL_LAYER_CELL_ID").put("PHYSICAL_LAYER_CELL_ID", aValoresParametros[mapaCabecera.get("PCI")]);
						}
						
						if(!mapaParametroValor.containsKey("LTE_CGI")){
							mapaParametroValor.put("LTE_CGI", new LinkedHashMap<String,String>());
							mapaParametroValor.get("LTE_CGI").put("CI", aValoresParametros[mapaCabecera.get("LTE_CI")]);
						}
						
						if(!mapaParametroValor.get("LTE_CGI").containsKey("MCC")){
							mapaParametroValor.get("LTE_CGI").put("MCC", aValoresParametros[mapaCabecera.get("MCC")]);
						}
						
						if(!mapaParametroValor.get("LTE_CGI").containsKey("TAC")){
							mapaParametroValor.get("LTE_CGI").put("TAC", aValoresParametros[mapaCabecera.get("TAC")]);
						}
						
						if(!mapaParametroValor.get("LTE_CGI").containsKey("MNC")){
							mapaParametroValor.get("LTE_CGI").put("MNC", aValoresParametros[mapaCabecera.get("MNC")]);
						}
						
						if(!mapaParametroValor.get("LTE_CGI").containsKey("MNC_LENGTH")){
							mapaParametroValor.get("LTE_CGI").put("MNC_LENGTH", "2");
						}
						
						if(!mapaParametroValor.containsKey("ANTENNA")){
							mapaParametroValor.put("ANTENNA", new LinkedHashMap<String,String>());
							mapaParametroValor.get("ANTENNA").put("DIRECTION", aValoresParametros[mapaCabecera.get("Azimuth")]);
						}
						
						if(!mapaParametroValor.get("ANTENNA").containsKey("BEAM_WIDTH")){
							mapaParametroValor.get("ANTENNA").put("BEAM_WIDTH", aValoresParametros[mapaCabecera.get("Beamwidth")]);
						}
						
						if(!mapaParametroValor.get("ANTENNA").containsKey("HEIGHT")){
							mapaParametroValor.get("ANTENNA").put("HEIGHT", aValoresParametros[mapaCabecera.get("ALTURA")]);
						}
						
						listaMapasParametroValorPorRegistro4G.add(mapaParametroValor);

					}
				}
				
				sValoresParametros = br.readLine();
			}
					
		
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(panel, "Hay un problema con el fichero de entrada.", "", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	
	private String convierteCellName(String sCellName) {
		String [] aCellName = UtilidadesTexto.divideTextoEnTokens(sCellName, "_");
		String sNombrePrincipio="";
		String sNombreIntermedio ="";
		String sNombreFinal="";
		if((aCellName!=null)&&(aCellName.length>0)){
			for(String sNombreCelda : aCellName){
				if(sNombreCelda.matches("[0-9]*")){
					if(sNombreCelda.length()==3){
						sNombreFinal = sNombreCelda.substring(1, 3); 
					}
					if(sNombreCelda.length()==9){
						sNombrePrincipio = sNombreCelda.substring(0,7);
						sNombreIntermedio = sNombreCelda.substring(7,9);
					}
				}
			}
		}
		return sNombrePrincipio+"_"+sNombreIntermedio+"_"+sNombreFinal;
	}

	private TreeMap<String, Integer> retornaMapaCabecera(String sCabeceraFichero) {
		String[] aParametrosCabecera = UtilidadesTexto.divideTextoEnTokens(sCabeceraFichero, "\t");
		if ((aParametrosCabecera != null) && (aParametrosCabecera.length > 0)) {
			for (int iRecorreParametros = 0; iRecorreParametros < aParametrosCabecera.length; iRecorreParametros++) {
				if (!mapaCabecera.containsKey(aParametrosCabecera[iRecorreParametros])) {
					mapaCabecera.put(aParametrosCabecera[iRecorreParametros], iRecorreParametros);
				}
			}
		}
		return mapaCabecera;
	}
	
	private static ArrayList<Double> convierteUTM2Grados(String sUTM) {
		double latitude;
		double longitude;

		String[] parts = sUTM.split(" ");
		int Zone = Integer.parseInt(parts[0]);
		char Letter = parts[1].toUpperCase(Locale.ENGLISH).charAt(0);
		double Easting = Double.parseDouble(parts[2]);
		double Northing = Double.parseDouble(parts[3]);
		double Hem;
		if (Letter > 'M')
			Hem = 'N';
		else
			Hem = 'S';
		double north;
		if (Hem == 'S')
			north = Northing - 10000000;
		else
			north = Northing;
		latitude = (north / 6366197.724
				/ 0.9996
				+ (1
						+ 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) - 0.006739496742
								* Math.sin(
										north / 6366197.724 / 0.9996)
								* Math.cos(
										north / 6366197.724
												/ 0.9996)
								* (Math.atan(Math
										.cos(Math
												.atan((Math
														.exp((Easting - 500000)
																/ (0.9996 * 6399593.625
																		/ Math.sqrt(
																				(1 + 0.006739496742
																						* Math.pow(
																								Math.cos(
																										north / 6366197.724
																												/ 0.9996),
																								2))))
																* (1 - 0.006739496742
																		* Math.pow(
																				(Easting - 500000) / (0.9996
																						* 6399593.625
																						/ Math.sqrt((1 + 0.006739496742
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2)))),
																				2)
																		/ 2
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2)
																		/ 3))
														- Math.exp(-(Easting - 500000)
																/ (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))))
																* (1 - 0.006739496742
																		* Math.pow(
																				(Easting - 500000) / (0.9996
																						* 6399593.625
																						/ Math.sqrt((1 + 0.006739496742
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2)))),
																				2)
																		/ 2
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2)
																		/ 3)))
														/ 2
														/ Math.cos((north - 0.9996 * 6399593.625
																* (north / 6366197.724 / 0.9996
																		- 0.006739496742 * 3 / 4
																				* (north / 6366197.724 / 0.9996 + Math
																						.sin(2 * north
																								/ 6366197.724 / 0.9996)
																						/ 2)
																		+ Math.pow(0.006739496742 * 3 / 4, 2) * 5
																				/ 3 * (3
																						* (north / 6366197.724
																								/ 0.9996
																								+ Math
																										.sin(2 * north
																												/ 6366197.724
																												/ 0.9996)
																										/ 2)
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2))
																				/ 4
																		- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27
																				* (5 * (3
																						* (north / 6366197.724 / 0.9996
																								+ Math.sin(2 * north
																										/ 6366197.724
																										/ 0.9996) / 2)
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2))
																						/ 4
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2)
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2))
																				/ 3))
																/ (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))))
																* (1 - 0.006739496742
																		* Math.pow(
																				(Easting - 500000) / (0.9996
																						* 6399593.625
																						/ Math.sqrt((1 + 0.006739496742
																								* Math.pow(
																										Math.cos(
																												north / 6366197.724
																														/ 0.9996),
																										2)))),
																				2)
																		/ 2
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))
																+ north / 6366197.724 / 0.9996)))
										* Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
												- 0.006739496742 * 3 / 4
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 4
												- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27
														* (5 * (3 * (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
																+ Math.sin(2 * north / 6366197.724 / 0.9996) * Math
																		.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
																/ 4
																+ Math.sin(2 * north / 6366197.724 / 0.9996)
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2)
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))
														/ 3))
												/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742
														* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
																2)
														/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												+ north / 6366197.724 / 0.9996))
										- north / 6366197.724
												/ 0.9996)
								* 3 / 2)
						* (Math.atan(Math
								.cos(Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625
										/ Math.sqrt((1 + 0.006739496742
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
										* (1 - 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
												/ 3))
										- Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742
														* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
																2)
														/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)))
										/ 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
												- 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 4
												- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 4
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 3))
												/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742
														* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
																2)
														/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												+ north / 6366197.724 / 0.9996)))
								* Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
										- 0.006739496742 * 3 / 4
												* (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
										+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3
												* (3 * (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 4
										- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5
												* (3 * (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 4
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 3))
										/ (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
										* (1 - 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										+ north / 6366197.724 / 0.9996))
								- north / 6366197.724 / 0.9996))
				* 180 / Math.PI;
		latitude = Math.round(latitude * 10000000);
		latitude = latitude / 10000000;
		longitude = Math
				.atan((Math
						.exp((Easting - 500000)
								/ (0.9996 * 6399593.625
										/ Math.sqrt(
												(1 + 0.006739496742
														* Math.pow(
																Math.cos(
																		north / 6366197.724
																				/ 0.9996),
																2))))
								* (1 - 0.006739496742
										* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))),
												2)
										/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))
						- Math.exp(
								-(Easting - 500000)
										/ (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
										* (1 - 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625 / Math
																.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
												/ 3)))
						/ 2
						/ Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
								- 0.006739496742 * 3 / 4
										* (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
								+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3
										* (3 * (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 4
								- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5
										* (3 * (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 4
										+ Math.sin(2 * north / 6366197.724 / 0.9996)
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 3))
								/ (0.9996 * 6399593.625
										/ Math.sqrt((1 + 0.006739496742
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
								* (1 - 0.006739496742
										* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))),
												2)
										/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
								+ north / 6366197.724 / 0.9996))
				* 180 / Math.PI + Zone * 6 - 183;
		longitude = Math.round(longitude * 10000000);
		longitude = longitude / 10000000;
		ArrayList<Double> retorno = new ArrayList<Double>();
		retorno.add(longitude);
		retorno.add(latitude);
		return retorno;

	}

	public static void main(String[] args) {
//		 if (args.length <= 0) {
//		 System.out.println("Falta parametro de nombre de configuración");
//		 return;
//		 }
//		
//		 try {
//		 ProcesaTexto procesa = new ProcesaTexto(args[0]);
//		 procesa.start();
//		
//		 } catch (ExcepcionEnTrabajo e) {
//		 System.out.println("ERROR: " + e.getMessage());
//		 }

	}

}
