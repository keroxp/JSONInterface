package me.keroxp.lib.jsoninterface;

import java.util.ArrayList;

/**
 * Created by keroxp on 2014/12/22.
 */
public class DataScheme extends JSONInterface {
    protected String format;
    protected ArrayList<User> data;
    static class User extends JSONInterface{
        protected String identifier;
        protected String name;
        protected Integer age;
        protected Float tall;
        protected String birth_of_date;
        protected String sex;
        public enum Sex { Unknown, Male, Female, Complex }
        public Sex getSex() {
            if (sex == null) return Sex.Unknown;
            if (sex.equals("male")) {
                return Sex.Male;
            }else if (sex.equals("female")) {
                return Sex.Female;
            }else if (sex.equals("complex")) {
                return Sex.Complex;
            }
            return Sex.Unknown;
        }

        @Override
        public String getJavaFieldName(String attr) {
            if (attr.equals("id")) {
                return "identifier";
            }
            return super.getJavaFieldName(attr);
        }

        @Override
        public String getJSONFieldName(String field) {
            if (field.equals("identifier")) {
                return "id";
            }
            return super.getJSONFieldName(field);
        }
    }
}
