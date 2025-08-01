package com.example.tournote.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tournote.Database.LocalDatabase.UserEntity

@Database(
    entities = [
        GroupEntity::class,
        UserEntity::class,
        GroupMemberCrossRef::class,
        GroupAdminCrossRef::class,
        GroupTrackFriendCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class TourNoteDatabase : RoomDatabase() {
    abstract fun tourNoteDao(): TourNoteDao

    companion object {
        @Volatile
        private var INSTANCE: TourNoteDatabase? = null

        fun getDatabase(context: Context): TourNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TourNoteDatabase::class.java,
                    "tour_note_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}