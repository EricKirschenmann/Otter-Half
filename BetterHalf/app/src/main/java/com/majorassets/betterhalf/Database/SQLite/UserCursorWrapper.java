package com.majorassets.betterhalf.Database.SQLite;

import android.database.Cursor;
import android.database.CursorWrapper;
import com.majorassets.betterhalf.Database.SQLite.UserDBSchema.UserDBTable;
import com.majorassets.betterhalf.Model.User;

import java.util.UUID;

/**
 * Created by dgbla on 3/30/2016.
 */
public class UserCursorWrapper extends CursorWrapper
{
    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public UserCursorWrapper(Cursor cursor)
    {
        super(cursor);
    }

    public User getUser()
    {
        String userID = getString(getColumnIndex(UserDBTable.Cols.UUID));
        String email = getString(getColumnIndex(UserDBTable.Cols.EMAIL));
        String password = getString(getColumnIndex(UserDBTable.Cols.PASSWORD));

        User user = new User(UUID.fromString(userID));
        user.setEmail(email);
        user.setPassword(password);

        return user;
    }
}
