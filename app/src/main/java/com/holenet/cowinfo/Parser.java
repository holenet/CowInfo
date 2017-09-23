package com.holenet.cowinfo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    static String getMetaDataHTML(String res, String name) {
        try {
            Document doc = Jsoup.parse(res);
            Elements metas = doc.select("meta[name="+name+"]");
            if(metas.size()==0)
                return null;
            Element meta = metas.get(0);
            return meta.attr("content");
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
/*

    static List<String[]> getFileListHTML(String res) {
        try {
            List<String[]> fileItems = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements files = doc.select("div[class=post]");
            for(Element file: files) {
                String filePath = file.child(1).attr("href");
                String author = file.child(2).text().split(": ")[1];
                String description = file.child(3).text().split(": ")[1];
                String datetime = file.child(4).text().split(": ")[1];
                fileItems.add(new String[] {filePath, author, description, datetime});
            }
            return fileItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<Post> getPostListHTML(String res) {
        try {
            List<Post> postItems = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements posts = doc.select("div[class=post]");
            for(Element post: posts) {
                String datetime = post.child(0).child(0).text();
                String title = post.child(1).text();
                String text = post.child(2).text();
                int sepIndex = datetime.indexOf(',', 10);
                Post postItem = new Post(0, title, "", new String[] {datetime.substring(0, sepIndex), datetime.substring(sepIndex+2)}, text);
                postItem.setCommentCount(Integer.valueOf(post.text().split(":")[post.text().split(":").length-1].replace(" ", "")));
                postItems.add(postItem);
            }
            return postItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
*/
    static List<String> getErrorListHTML(String res) {
        try {
            List<String> errors = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements errorLists = doc.select("ul[class=errorlist]");
            for(Element errorList: errorLists) {
                String error = errorList.child(0).text();
                errors.add(0, error);
                Log.e("error", error);
            }
            return errors;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
/*
    static Post getPostJSON(String res) {
        Post post = null;
        try {
            JSONObject jo = new JSONObject(res);
            JSONArray ja = jo.getJSONArray("comments");
            List<Comment> comments = new ArrayList<>();
            for(int i=0; i<ja.length(); i++) {
                JSONObject o = ja.getJSONObject(i);
                comments.add(new Comment(o.getInt("id"), o.getString("author"), getDatetime(o.getString("datetime")), o.getString("text")));
            }
            post = new Post(jo.getInt("id"), jo.getString("title"), jo.getString("author"), getDatetime(jo.getString("datetime")), jo.getString("text"), comments);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return post;
    }
*/

    private static String[] getDatetime(String res) {
        return new String[] {res.split(" ")[0], res.split(" ")[1].split("\\.")[0]};
    }
}
