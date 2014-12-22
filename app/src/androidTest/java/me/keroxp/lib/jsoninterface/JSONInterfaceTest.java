package me.keroxp.lib.jsoninterface;

import android.app.Application;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.test.ApplicationTestCase;
import android.util.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class JSONInterfaceTest extends AndroidTestCase {
    @Override
    protected void setUp () throws Exception {
        super.setUp();
    }
    public void testDeserialize () throws Exception {
        InputStream is = getContext().getResources().openRawResource(R.raw.mock);
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while((bis.read(buf) != -1)) {
            bao.write(buf);
        }
        byte[] bytes = bao.toByteArray();
        String jsonstr = new String(bytes);
        JSONObject json = new JSONObject(jsonstr);
        DataScheme data = (DataScheme) JSONInterface.instantiate(DataScheme.class, json);
        assertNotNull(data);
        assertTrue(data.format.equals("json"));
        DataScheme.User john = data.data.get(0);
        assertEquals(john.name, "John");
        assertEquals(john.getSex(), DataScheme.User.Sex.Male);
    }
    static class TestScheme extends JSONInterface {
        protected String name;
        protected Integer age;
        protected ArrayList<String> favorites;
    }
    public void testSerialize () throws Exception {
        TestScheme test = new TestScheme();
        test.name = "keroxp";
        test.age = 23;
        ArrayList<String> favs = new ArrayList<>();
        favs.add("Blossom");
        favs.add("Marine");
        favs.add("Sunshine");
        favs.add("Moonlight");
        test.favorites = favs;
        StringWriter stringWriter = new StringWriter();
        JSONInterface.serializeToJSON(new JsonWriter(stringWriter), test);
        String jsonstr = stringWriter.toString();
        assertTrue(jsonstr.length() > 0);
        TestScheme reversed = (TestScheme) JSONInterface.instantiate(TestScheme.class, new JSONObject(jsonstr));
        assertEquals(reversed.name, test.name);
        assertEquals(reversed.age, test.age);
        for (int i = 0, max = favs.size(); i < max; ++i) {
            assertEquals(reversed.favorites.get(i),test.favorites.get(i));
        }
    }
    @Override
    protected void tearDown () throws Exception {
        super.tearDown();
    }
}