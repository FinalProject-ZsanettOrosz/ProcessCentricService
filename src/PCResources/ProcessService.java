package PCResources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

@Stateless
@LocalBean
@Path("/person")
public class ProcessService {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	private String doPost(String url, String postedValue) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(url);
		StringBuffer result = null;
		try {
			request.setEntity(new StringEntity(postedValue));
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result.toString();
	}

	private String doGet(String url) throws ClientProtocolException,
			IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);

		BufferedReader rd = new BufferedReader(new InputStreamReader(response
				.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return result.toString();
	}

	@POST
	@Path("/{personID}/{measureType}")
	@Produces({ MediaType.TEXT_PLAIN })
	@Consumes({ MediaType.TEXT_PLAIN })
	public Response newLifeStatus(@PathParam("personID") int personID,
			@PathParam("measureType") String measureType, String postedValue) {

		System.out.println("PC - in POST");
		String[] congrats = null;
		String toMotivate = "";
		try {

			// integration logic
			// POST measure to BL
			// GET achivements for today
			// GET goals from SS
			// GET pic if there were achivements

			String strUrl = "https://intense-mesa-6521.herokuapp.com/sdelab/goals/person/"
					+ personID;
			String strAch = doGet(strUrl);
			JSONArray achBeforeArray = new JSONArray(strAch);
			int beforePost = achBeforeArray.length();
			System.out.println(beforePost);

			// POST to BL
			String urlToPost = "https://secret-forest-8470.herokuapp.com/sdelab/person/"
					+ personID + "/" + measureType;
			String afterPostValue = doPost(urlToPost, postedValue);
			System.out.println("person after post: " + afterPostValue);

			String strUrlAfterPost = "https://intense-mesa-6521.herokuapp.com/sdelab/goals/person/"
					+ personID;
			String strAchAfter = doGet(strUrlAfterPost);
			JSONArray achAfterArray = new JSONArray(strAchAfter);
			int afterPost = achAfterArray.length();
			System.out.println(afterPost);

			// GET achivements fot today from SS
			// this is all the achivements so far
			String urlForAchivements = "https://intense-mesa-6521.herokuapp.com/sdelab/goals/person/"
					+ personID;
			String achivements = doGet(urlForAchivements);

			System.out.println("achivements: " + achivements);

			JSONArray achArray = new JSONArray(achivements);
			JSONArray achivedToday = new JSONArray();
			for (int i = 0; i < achArray.length(); i++) {
				JSONObject ach = new JSONObject(achArray.getJSONObject(i)
						.toString());
				Date today = new Date();

				String strDate = ach.getString("achivementDate");
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				Date df = format.parse(strDate);

				if (today.getYear() == df.getYear()
						&& today.getMonth() == df.getMonth()
						&& today.getDay() == df.getDay()) {
					achivedToday.put(ach);
					System.out.println("Today:" + ach.toString(4));
				}
			}
			System.out.println("Achived goals today: " + achivedToday.length());

			// GET from SS
			String urlForGoals = "https://intense-mesa-6521.herokuapp.com/sdelab/goals/";
			String goals = doGet(urlForGoals);
			JSONArray allGoals = new JSONArray(goals);
			JSONArray goalsToCongratulate = new JSONArray();

			

			for (int j = 0; j < achivedToday.length(); j++) {
				JSONObject achived = new JSONObject(achivedToday.getJSONObject(
						j).toString());
				JSONObject goalInAchivedGoal = new JSONObject(achived.get(
						"achivedGoal").toString());

				for (int i = 0; i < allGoals.length(); i++) {
					JSONObject goal = new JSONObject(allGoals.getJSONObject(i)
							.toString());
					if (goalInAchivedGoal.getInt("idGoal") == goal
							.getInt("idGoal") ) {
						JSONObject m = new JSONObject(goal.get("measureDef").toString());
						if(m.get("name").equals(measureType)){
							goalsToCongratulate.put(goal); // only if its in the posted type
							System.out.println(goal.toString());
						}
						
					}
				}

			}
			

			// GET pic for every goal and congratulate
			/*if (beforePost < afterPost) {
				toMotivate = "Keep going!";
				System.out.println(toMotivate);
			} else {*/

				congrats = new String[goalsToCongratulate.length()];

				for (int i = 0; i < goalsToCongratulate.length(); i++) {

					String urlForPic = "https://intense-mesa-6521.herokuapp.com/sdelab/goals/pic";
					String pic = doGet(urlForPic);
					System.out.println(pic);

					JSONObject g = new JSONObject(goalsToCongratulate
							.getJSONObject(i).toString());
					JSONObject md = new JSONObject(g.get("measureDef")
							.toString());

					System.out.println(g.toString(4));
					System.out.println(md.toString(4));

					congrats[i] = "Congratulations! You have achived the minimum "
							+goalsToCongratulate.getJSONObject(i).getDouble(
									"goalValue")
							+ " "
							+ md.getString("name")
							+ " today! Here is a pretty picture or you, as a revard:"
							+ pic;
					System.out.println(congrats[i]);
					toMotivate += congrats[i];
				}
				
				System.out.println(toMotivate);
			//}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Response.ok(toMotivate).build();
	}
}
