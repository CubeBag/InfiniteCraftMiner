
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

	public Main() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

		HttpClient client = newClientFromArgs(args); // coal
		final List<String> knownWords;

		final JSONObject interestBoredom;
		final AttemptTracker attemptTracker;
		final RecipeBook recipeBook;

		final int newDiscoveryInterestStart = 30;
		final int oldDiscoveryInterestStart = 5;
		final int oldDiscoveryComboInterestUp = 2;
		final int newDiscoveryComboInterestUp = 3;
		final int noDiscoveryComboBoredomUp = 1;
		final int defaultInterestStart = 2;
		final int defaultBoredomStart = 2;

		Scanner in = new Scanner(System.in);

		System.out.println("Select Your Mode");
		System.out.println("a : Type two and see the result");
		System.out.println("b : Try them randomly and store results to file");
		System.out.println("c : Same as b, but resume from file as well");
		System.out.println("d : Create a website from progress file");
		System.out.println("e : Combine two progress files");
		System.out.println("f : Seed fresh progress file with elements");
		System.out.println("g : Validate all recipes");
		System.out.println("h : Prune elements");

		int killCount = 0;

		String input = in.nextLine();

		if (input.charAt(0) == 'h') {
			System.out.println("This Prunes & Purges elements from the progress file.");
			System.out.println("It will remove:");
			System.out.println("- The elements you type");
			System.out.println("- The elements whose recipes include the elements you type");
			System.out.println("- The elements you type");

		}

		else if (input.charAt(0) == 'g') {
			System.out.print("Filename? ");

			String filename = in.nextLine();

			JSONObject j = readJSONObjectFile(filename);
			JSONObject recipes = j.getJSONObject("recipes");
			int total = recipes.length();
			int valid = 0;
			List<String> badElems = new ArrayList<>();
			for (String elem : recipes.keySet()) {

				JSONArray recipe = recipes.getJSONArray(elem);
				JSONObject test = tryCombination(recipe.getString(0), recipe.getString(1), client);
				String result = (String) test.get("result");
				if (elem.equals(result)) {
					valid++;
					System.out.println("Validated: " + result + " = " + recipe.getString(0) + " + " + recipe.getString(1) + " (" + valid + "/" + total + ")");
				} else {
					System.out.println(
							"COULD NOT VALIDATE: " + recipe.getString(0) + " + " + recipe.getString(1) + " (expected " + elem + ", got " + result + ")");
					badElems.add(elem);
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			System.out.println("*** Report for file " + filename);
			if (valid == total) {
				System.out.println("All " + valid + " recipes validated.");
			} else {
				System.out.println("*************WARNING******************");
				System.out.println("COULD NOT validate recipes for these " + (total - valid) + " elements:");
				System.out.println(badElems.toString());
				System.out.println("The INVALID recipes are as follows:");
				for (String r : badElems) {
					System.out.println(" " + r + "=" + recipes.getJSONArray(r));
				}
				System.out.println("*************WARNING******************");
			}

		}

		else if (input.charAt(0) == 'f')

		{
			System.out.println("This creates a new progess (.json) file with your own elements of choosing!");
			System.out.println("However, this will NOT fabricate the formulas/recipes for what you type!");
			System.out.println("Do not COMBINE this (e) with any other progress files unless you know");
			System.out.println("you have a recipe in another file!!");
			System.out.println("");
			System.out.print("Filename? ");

			String filename = in.nextLine();

			attemptTracker = new AttemptTracker();
			recipeBook = new RecipeBook();
			knownWords = new ArrayList<>();
			interestBoredom = new JSONObject();
//            knownWords.add("Water");
//            knownWords.add("Fire");
//            knownWords.add("Wind");
//            knownWords.add("Earth");
//            interestBoredom.put("Water", makeJSONArray(2, 2));
//            interestBoredom.put("Fire", makeJSONArray(2, 2));
//            interestBoredom.put("Wind", makeJSONArray(2, 2));
//            interestBoredom.put("Earth", makeJSONArray(2, 2));

			System.out.print("Element? (Blank = save & exit) ");
			String elem = in.nextLine();

			while (elem.length() > 0) {
				knownWords.add(elem);
				interestBoredom.put(elem, makeJSONArray(10, 2));

				System.out.print("Element? (Blank = save & exit) ");
				elem = in.nextLine();

			}

			exportFile(attemptTracker.exportJson(), exportKnownJson(knownWords), recipeBook.exportJson(), interestBoredom, filename);

		}

		else if (input.charAt(0) == 'e') {
			System.out.print("PRIMARY progress file name? ");
			String prim = in.nextLine();
			System.out.print("SECONDARY progress file name? ");
			String sec = in.nextLine();
			System.out.print("OUTPUT progress file name? ");
			String outp = in.nextLine();
			System.out.println("Import interest/boredom? (y/n)");
			boolean importIB = in.nextLine().charAt(0) == 'y';

			JSONObject primJson = readJSONObjectFile(prim);
			JSONObject secJson = readJSONObjectFile(sec);

			List<String> primElements = knownJsonToList(primJson.getJSONArray("knownElements"));
			List<String> secElements = knownJsonToList(secJson.getJSONArray("knownElements"));
			RecipeBook primRecipe = new RecipeBook(primJson.getJSONObject("recipes"));
			RecipeBook secRecipe = new RecipeBook(secJson.getJSONObject("recipes"));

			while (secElements.size() > 0) {
				String elem = secElements.remove(0);
				if (!primElements.contains(elem)) {
					Tuple<String, String> recipe = secRecipe.getRecipe(elem);
					if (primElements.contains(recipe.x) && primElements.contains(recipe.y)) {
						primElements.add(elem);
						primRecipe.addEntry(elem, recipe.x, recipe.y);
					} else {
						secElements.add(elem);
					}
				} else {
					// do nothing, it's already in primElements
				}
			}

			AttemptTracker primAttempts = new AttemptTracker(primJson.getJSONArray("triedCombinations"));
			JSONArray secAttemptsJ = secJson.getJSONArray("triedCombinations");

			for (int i = 0; i < secAttemptsJ.length(); i++) {
				JSONArray cur = secAttemptsJ.getJSONArray(i);
				primAttempts.isNewCombination(cur.getString(0), cur.getString(1));
			}

			JSONObject primIB = primJson.getJSONObject("interestBoredom");
			if (importIB) {
				JSONObject secIB = secJson.getJSONObject("interestBoredom");
				for (String k : secIB.keySet()) {
					// System.out.println(k);
					if (!primIB.has(k)) {
						primIB.put(k, secIB.getJSONArray(k));
					} else {
						JSONArray primIBindiv = primIB.getJSONArray(k);
						JSONArray secIBindiv = secIB.getJSONArray(k);
						primIBindiv.put(0, secIBindiv.getInt(0) + primIBindiv.getInt(0));
						primIBindiv.put(1, secIBindiv.getInt(1) + primIBindiv.getInt(1));

					}
				}
			}

			Main.exportFile(primAttempts.exportJson(), exportKnownJson(primElements), primRecipe.exportJson(), primIB, outp);

		}

		else if (input.charAt(0) == 'd') {
			System.out.print("Progress file name? (include .json)");
			String prog = in.nextLine();
			System.out.println("Folder name to store site?");
			String folderName = in.nextLine();

			JSONObject dataJson = readJSONObjectFile(prog);

			WebsiteBuilder.createWebsite(folderName, dataJson.getJSONArray("knownElements"), dataJson.getJSONObject("recipes"));

			System.out.println("Done");

		} else if (input.charAt(0) == 'a') {
			System.out.println();

			while (true) {
				System.out.print("First element? ");
				String first = in.nextLine();
				System.out.print("Second element? ");
				String second = in.nextLine();
				JSONObject output = tryCombination(first, second, client);
				if (output.get("result").equals("Nothing")) {
					System.out.println("You get NOTHING");
				} else {
					System.out.println("The result is " + output.get("result"));
					System.out.println("The emoji is " + output.get("emoji"));
					if (((Boolean) output.get("isNew"))) {
						System.out.println("You are the FIRST DISCOVERER of this element lol");
					}
				}
				System.out.println();
				System.out.println();

			}

		} else if (input.charAt(0) == 'b' || input.charAt(0) == 'c') {
			boolean interestBoredomSelected;
			System.out.println("Use Interest/Boredom random selection weighting? (y = yes, n = normal random)");
			interestBoredomSelected = (in.nextLine().charAt(0) == 'y');

			System.out.println("Name of progress file? (Recommend end in .json)");
			String resumeData = in.nextLine();

			System.out.println("What shall be the delay between attempts? (In milliseconds) (entering 0 here WILL get you IP banned!!)");
			String delayStr = in.nextLine();
			int delay = Integer.parseInt(delayStr);

			if (input.charAt(0) == 'c') {
				JSONObject dataJson = readJSONObjectFile(resumeData);

				attemptTracker = new AttemptTracker(dataJson.getJSONArray("triedCombinations"));
				knownWords = knownJsonToList(dataJson.getJSONArray("knownElements"));
				if (dataJson.has("interestBoredom")) {
					interestBoredom = dataJson.getJSONObject("interestBoredom");
				} else {
					interestBoredom = new JSONObject();
				}
				recipeBook = new RecipeBook(dataJson.getJSONObject("recipes"));

			} else {
				attemptTracker = new AttemptTracker();
				recipeBook = new RecipeBook();
				knownWords = new ArrayList<>();
				interestBoredom = new JSONObject();
				knownWords.add("Water");
				knownWords.add("Fire");
				knownWords.add("Wind");
				knownWords.add("Earth");
				interestBoredom.put("Water", makeJSONArray(2, 2));
				interestBoredom.put("Fire", makeJSONArray(2, 2));
				interestBoredom.put("Wind", makeJSONArray(2, 2));
				interestBoredom.put("Earth", makeJSONArray(2, 2));

			}

			JSONObject debugObject = new JSONObject();
			String first = "snd";
			String second = "asmd";

			try {

				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						System.out.println("Interrupt Found! I will save and quit now.");

						exportFile(attemptTracker.exportJson(), exportKnownJson(knownWords), recipeBook.exportJson(), interestBoredom, resumeData);

					}
				});
				System.out.println("File prefix? (Can use forward slash to specify subdirectory. Make it blank for no prefix i.e. current directory)");

				String prefix = in.nextLine();

				BufferedWriter writer = new BufferedWriter(new FileWriter(prefix + "foundThings_" + System.currentTimeMillis() + ".txt", false));

				while (true) {

					boolean newTry = false;

					first = "Fire";
					second = "Water";

					while (!newTry) {

						interestBoredomSelected = true;
						// TODO : add selector for this bool
						String[] firstSecond;

						if (interestBoredomSelected) {
							firstSecond = randomComboWeightedInterestBoredom(knownWords, interestBoredom);

						} else {

							firstSecond = randomCombo(knownWords);
						}

						first = firstSecond[0];
						second = firstSecond[1];

						// System.out.println("Random selection: "+ Arrays.toString(firstSecond));

						newTry = attemptTracker.isNewCombination(first, second);

					}

					JSONObject json = tryCombination(first, second, client);

					debugObject = json;

					// System.out.println("Result: " + json.get("result"));
					String result = (String) json.get("result");

					boolean newDiscovery = false;
					boolean discovery = false;

					if (result.contains("%20")) {
						System.out.println("👺👺👺👺👺👺 BURNED RESULT 👺👺👺 %20 VICTIM");
						System.out.println("***" + first + " + " + second + " = " + result);
						System.out.println("***This is a FAKE discovery.");
						System.out.println(json);
						System.out.println("***The combination will not be tried again.");

					} else if (!result.equals("Nothing")) {

						String outString = first + " + " + second + " = " + result;
						if (json.has("emoji")) {
							outString += "          " + (String) (json.get("emoji"));
						} else {
							outString += "          " + "-";
							// sometimes the emoji field is literally absent
							// which is super fucking weird!!!!
						}
						if (((Boolean) json.get("isNew"))) {
							newDiscovery = true;
							outString += "          {NEW}";
							killCount += 1;
							System.out.println("* NEW KILL *  (this session: " + killCount + ")");
						}

						System.out.println(outString);

						if (!knownWords.contains(result)) {
							knownWords.add(result);
							writer.append(outString);
							writer.newLine();
							writer.flush();
							recipeBook.addEntry(result, first, second);
							discovery = true;

						}
					} else {

						System.out.println(first + " + " + second + " = " + "nothing LLOL trying again");
					}

					if (!interestBoredom.has(first)) {
						JSONArray ibFirst = new JSONArray();
						ibFirst.put(defaultInterestStart);
						ibFirst.put(defaultBoredomStart);
						interestBoredom.put(first, ibFirst);
					}

					if (!interestBoredom.has(second)) {
						JSONArray ibSecond = new JSONArray();
						ibSecond.put(defaultInterestStart);
						ibSecond.put(defaultBoredomStart);
						interestBoredom.put(second, ibSecond);
					}

					JSONArray ib1 = interestBoredom.getJSONArray(first);
					JSONArray ib2 = interestBoredom.getJSONArray(second);

					if (newDiscovery) {

						interestBoredom.put(result, makeJSONArray(newDiscoveryInterestStart, defaultBoredomStart));

						ib1.put(0, ib1.getInt(0) + newDiscoveryComboInterestUp);
						ib2.put(0, ib2.getInt(0) + newDiscoveryComboInterestUp);
					} else if (discovery) {

						interestBoredom.put(result, makeJSONArray(oldDiscoveryInterestStart, defaultBoredomStart));

						ib1.put(0, ib1.getInt(0) + oldDiscoveryComboInterestUp);
						ib2.put(0, ib2.getInt(0) + oldDiscoveryComboInterestUp);

					} else {
						ib1.put(1, ib1.getInt(1) + noDiscoveryComboBoredomUp);
						ib2.put(1, ib2.getInt(1) + noDiscoveryComboBoredomUp);
					}

					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}

				}
			} catch (Exception e) {
				System.out.println("Oops! There was a fucky wucky! Saving and exiting NOW...");
				e.printStackTrace();
				System.out.println(e);
				System.out.println("Current json: " + debugObject);
				System.out.println("first: " + first);
				System.out.println("second: " + second);

				exportFile(attemptTracker.exportJson(), exportKnownJson(knownWords), recipeBook.exportJson(), interestBoredom, resumeData);
				throw (e);
			}
		}

	}

	public static void exportFile(JSONArray combinations, JSONArray knownElements, JSONObject recipes, JSONObject interestBoredom, String file) {
		JSONObject export = new JSONObject();
		export.put("triedCombinations", combinations);
		export.put("knownElements", knownElements);
		export.put("recipes", recipes);
		export.put("interestBoredom", interestBoredom);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
			writer.append(export.toString());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static JSONObject tryCombination(String a, String b, HttpClient client) throws URISyntaxException, IOException, InterruptedException {

		String uri = "https://neal.fun/api/infinite-craft/pair";
		uri += "?first=" + a.replace("%", "%25").replace(" ", "%20").replace("+", "%2B");
		uri += "&second=" + b.replace("%", "%25").replace(" ", "%20").replace("+", "%2B");

		URI combiner = new URI(uri);

		// System.out.println(uri);

		HttpRequest request = HttpRequest.newBuilder(combiner).GET()
				.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:122.0) Gecko/20100101 Firefox/122.0")
				.header("Referer", "https://neal.fun/infinite-craft/").header("Accept", "*/*").build();

		HttpResponse<String> response = null;
		while (response == null) {
			try {
				response = client.send(request, BodyHandlers.ofString());
			} catch (IOException e) {
				if (e.toString().contains("GOAWAY")) {
					System.out.println("Got GOAWAY, attempting to ignore and try again (details below)");
					System.out.println(e);
					Thread.sleep(100000);
				}
			}
		}

		while (response.statusCode() == 500) {

			System.out.println("Calming my titties...");
			client = newClientFromArgs(null);
			Thread.sleep(23530);
			request = HttpRequest.newBuilder(combiner).GET()
					.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:122.0) Gecko/20100101 Firefox/122.0")
					.header("Referer", "https://neal.fun/infinite-craft/").header("Accept", "*/*").build();
			System.out.println("request hashCode" + request.hashCode());
			System.out.println(request.headers());
			System.out.println(request.toString());

			response = client.send(request, BodyHandlers.ofString());
			System.out.println("response hashCode" + response.hashCode());
			System.out.println("response status code" + response.statusCode());

		}
		String responseBody = response.body();
		// System.out.println(responseBody);

		JSONObject json;

		try {
			json = new JSONObject(responseBody);
		} catch (JSONException e) {
			System.out.println("FUCK");
			e.printStackTrace();
			System.out.println(e);
			System.out.println("Response Body was" + responseBody);
			System.out.println("Response headers were " + response.headers());
			System.out.println("Response status code was: " + response.statusCode());
			throw (e);
		}

		return json;
	}

	public static JSONArray exportKnownJson(List<?> known) {
		JSONArray knownJson = new JSONArray();
		for (Object k : known) {
			String k2 = (String) k;
			knownJson.put(k2);
		}
		return knownJson;
	}

	public static List<String> knownJsonToList(JSONArray known) {
		int l = known.length();

		List<String> ret = new ArrayList<>();

		for (int i = 0; i < l; i++) {
			ret.add((String) known.get(i));
		}

		return ret;
	}

	public static String[] randomCombo(List<String> knownWords) {
		String first = knownWords.get((int) (Math.random() * knownWords.size()));
		String second = knownWords.get((int) (Math.random() * knownWords.size()));
		String[] ret = { first, second };
		return ret;
	}

	public static String[] randomComboWeightedInterestBoredom(List<String> knownWords, JSONObject weights) {

		double[] weightSeekTable = new double[knownWords.size()];

		double cumulative = 0.0;

		for (int i = 0; i < weightSeekTable.length; i++) {
			weightSeekTable[i] = cumulative;

			if (!weights.has(knownWords.get(i))) {
				// ALSO initialize to 2 if it ain't exist cause that's a problem
				weights.put(knownWords.get(i), makeJSONArray(2, 2));
			}

			int interest = weights.getJSONArray(knownWords.get(i)).getInt(0);
			int boredom = weights.getJSONArray(knownWords.get(i)).getInt(1);
			cumulative += ((double) interest) / boredom;

		}

		double choice1 = Math.random() * cumulative;
		double choice2 = Math.random() * cumulative;

		int index1 = -Arrays.binarySearch(weightSeekTable, choice1) - 2;
		int index2 = -Arrays.binarySearch(weightSeekTable, choice2) - 2;

		String first = knownWords.get(index1);
		String second = knownWords.get(index2);

		String[] ret = { first, second };
		return ret;
	}

	public static JSONArray makeJSONArray(int a, int b) {
		JSONArray ret = new JSONArray();
		ret.put(a);
		ret.put(b);
		return ret;

	}

	public static JSONObject readJSONObjectFile(String fileName) throws IOException {
		FileReader reader = new FileReader(new File(fileName));

		StringBuilder dataStrBuilder = new StringBuilder();

		int i = 0;

		while ((i = reader.read()) != -1) {
			dataStrBuilder.append((char) i);
		}

		String dataStr = dataStrBuilder.toString();
		JSONObject dataJson = new JSONObject(dataStr);

		reader.close();

		return dataJson;
	}

	public static HttpClient newClientFromArgs(String[] args) {
		HttpClient client;

		client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

		return client;

		// not really anything to do yet

	}

}
