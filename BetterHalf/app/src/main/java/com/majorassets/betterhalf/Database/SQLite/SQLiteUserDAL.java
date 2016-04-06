package com.majorassets.betterhalf.Database.SQLite;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.majorassets.betterhalf.Database.SQLite.UserDBSchema.UserDBTable;
import com.majorassets.betterhalf.GlobalResources;
import com.majorassets.betterhalf.Model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dgbla on 3/30/2016.
 */
public class SQLiteUserDAL implements ISQLiteUserDAL
{
    private SQLiteDatabase db;
    public SQLiteUserDAL(SQLiteDatabase db)
    {
        this.db = db;
    }

    public void addUser(User user)
    {
        ContentValues values = SQLiteProvider.getUserContentValues(user);
        try
        {
            db.insertOrThrow(UserDBTable.NAME, null, values);
        }
        catch(SQLException ex)
        {
            Log.d("ERROR", ex.getMessage());
        }
    }

    @Override
    public void updateUser(User user)
    {
        String userID = user.getID().toString();
        ContentValues values = SQLiteProvider.getUserContentValues(user);

        db.update(UserDBTable.NAME, values, UserDBTable.Cols.UUID + " = ?", new String[] {userID});
    }

    @Override
    public User deleteUser(User user)
    {
        return null;
    }

    @Override
    public User getUser(String email)
    {
        UserCursorWrapper cursor = SQLiteProvider.queryUser(UserDBTable.Cols.EMAIL + "= ?", new String[]{email});

        try
        {
            if(cursor.getCount() == 0){
                return null;
            }

            cursor.moveToFirst();
            return cursor.getUser();
        }
        finally{
            cursor.close();
        }
    }

    public List<User> getAllUsers()
    {
        UserCursorWrapper cursor = SQLiteProvider.queryUser(null, null);

        List<User> users = new ArrayList<>();
        try
        {
            if(cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            users.add(cursor.getUser());
        }
        finally
        {
            cursor.close();
        }

        return users;
    }
}
