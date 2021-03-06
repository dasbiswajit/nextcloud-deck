package it.niedermann.nextcloud.deck.persistence.sync.adapters.db.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import it.niedermann.nextcloud.deck.model.User;

@Dao
public interface UserDao extends GenericDao<User> {

    @Query("SELECT * FROM user WHERE accountId = :accountId")
    LiveData<List<User>> getUsersForAccount(final long accountId);

    @Query("SELECT * FROM user WHERE accountId = :accountId and localId = :localId")
    LiveData<User> getUserByLocalId(final long accountId, final long localId);

    @Query("SELECT * FROM user WHERE accountId = :accountId and uid = :uid")
    LiveData<User> getUserByUid(final long accountId, final String uid);

    @Query("SELECT * FROM user WHERE accountId = :accountId and ( uid LIKE :searchTerm or displayname LIKE :searchTerm or primaryKey LIKE :searchTerm )")
    LiveData<List<User>> searchUserByUidOrDisplayName(final long accountId, final String searchTerm);

    @Query("SELECT * FROM user WHERE accountId = :accountId and uid = :uid")
    User getUserByUidDirectly(final long accountId, final String uid);

    @Query("SELECT * FROM user WHERE localId IN (:assignedUserIDs) and status <> 3") // not LOCAL_DELETED
    List<User> getUsersByIdDirectly(List<Long> assignedUserIDs);

    @Query("SELECT * FROM user WHERE localId = :localUserId")
    User getUserByLocalIdDirectly(long localUserId);
}