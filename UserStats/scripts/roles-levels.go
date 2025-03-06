'''
Snap4city -- Roles - Levels calculator --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''
package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"gopkg.in/ldap.v3"
	"log"
	"strconv"
	"strings"
)

func checkErr(err error) {
	if err != nil {
		panic(err)
	}
}

/*func main1() {
// insert
        stmt, err := db.Prepare("INSERT userinfo SET username=?,departname=?,created=?")
        checkErr(err)

        res, err := stmt.Exec("astaxie", "研发部门", "2012-12-09")
        checkErr(err)

        id, err := res.LastInsertId()
        checkErr(err)

        fmt.Println(id)
        // update
        stmt, err = db.Prepare("update userinfo set username=? where uid=?")
        checkErr(err)

        res, err = stmt.Exec("astaxieupdate", id)
        checkErr(err)

        affect, err := res.RowsAffected()
        checkErr(err)

        fmt.Println(affect)

        // query
        rows, err := db.Query("SELECT * FROM userinfo")
        checkErr(err)

        for rows.Next() {
            var uid int
            var username string
            var department string
            var created string
            err = rows.Scan(&uid, &username, &department, &created)
            checkErr(err)
            fmt.Println(uid)
            fmt.Println(username)
            fmt.Println(department)
            fmt.Println(created)
        }

        // delete
        stmt, err = db.Prepare("delete from userinfo where uid=?")
        checkErr(err)

        res, err = stmt.Exec(id)
        checkErr(err)

        affect, err = res.RowsAffected()
        checkErr(err)

        fmt.Println(affect)

        db.Close()

    }*/

// check if a string is a number
func is_number(s string) bool {
	if _, err := strconv.ParseFloat(s, 64); err == nil {
		fmt.Printf("%q looks like a number.\n", s)
		return true
	} else {
		return false
	}
}

// check if a string is present in a slice
func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

// get users => roles map from LDAP
func getUsersRoles() map[string]string {
	users_roles := map[string]string{}

	l, err := ldap.Dial("tcp", fmt.Sprintf("%s:%d", "localhost", 389))
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()

	err = l.Bind("cn=admin,dc=ldap,dc=disit,dc=org", "C1,*sv3#")
	if err != nil {
		log.Fatal(err)
	}

	//var pageSize uint32 = 32
	//pagingControl := ldap.NewControlPaging(pageSize)
	//controls := []ldap.Control{pagingControl}

	searchRequest := ldap.NewSearchRequest(
		"dc=ldap,dc=disit,dc=org", // The base dn to search
		ldap.ScopeWholeSubtree, ldap.NeverDerefAliases, 0, 0, false,
		"(objectClass=organizationalRole)", // The filter to apply
		[]string{"roleOccupant"},           // A list attributes to retrieve
		nil,
	)

	sr, err := l.Search(searchRequest)
	if err != nil {
		log.Fatal(err)
	}

	for _, entry := range sr.Entries {
		dn := strings.Split(entry.DN, ",")
		for _, attr := range entry.Attributes {
			for _, entryAttribute := range attr.Values {
				role := strings.Split(dn[0], "=")[1]
				user := strings.Split(entryAttribute, ",")[0]

				if user != "" {
					user = strings.Split(user, "=")[1]
					users_roles[user] = role
				}
			}
		}
	}
	return users_roles
}

// get users who created dashboards through device IoT broker
func getBrokerDashboardUsers() []string {
	result := make([]string, 0)

	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/Dashboard")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close until the main function has finished executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT t1.user FROM (SELECT a.Id, a.creator, b.user, a.id_dashboard, a.creationDate, b.creation_date " +
		"FROM Dashboard.Config_widget_dashboard a " +
		"LEFT JOIN Dashboard.Config_dashboard b ON a.id_dashboard = b.Id " +
		"WHERE actuatorTarget = 'broker' AND b.deleted = 'no') t1 " +
		"WHERE t1.Id = (SELECT t2.Id FROM (SELECT a.Id, a.creator, b.user, a.id_dashboard, a.creationDate, b.creation_date " +
		"FROM Dashboard.Config_widget_dashboard a " +
		"LEFT JOIN Dashboard.Config_dashboard b ON a.id_dashboard = b.Id " +
		"WHERE actuatorTarget = 'broker' AND b.deleted = 'no') t2 WHERE t2.user = t1.user ORDER BY t2.Id DESC LIMIT 1)")

	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	for results.Next() {
		var user string
		// for each row, scan the result into our tag composite object
		err = results.Scan(&user)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result = append(result, user)
	}
	return result
}

func getUsersOrganizations() map[string][]string {
	result := map[string][]string{}
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/snap4citydb")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close until the main function has finished executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT a.name, c.title FROM snap4citydb.users a LEFT JOIN snap4citydb.og_membership b ON a.uid = b.etid LEFT JOIN snap4citydb.node c ON b.gid = c.nid WHERE b.entity_type='user'")

	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	for results.Next() {
		var name, title string
		// for each row, scan the result into our tag composite object
		err = results.Scan(&name, &title)
		if err != nil {
			panic(err.Error()) // proper error handling instead of panic in your app
		}
		result[name] = append(result[name], title)
	}
	return result
}

func getUsersLevels(users_roles map[string]string, brokerDashboardUsers []string) map[string]int {
	users_levels := map[string]int{}
	var level int
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close until the main function has finished executing
	defer db.Close()

	// Execute the query
	results, err := db.Query("SELECT t1.username, t1.dashboards_public, t1.dashboards_private, " +
		"t1.devices_public, t1.devices_private, t1.iot_apps, t1.etl_writes FROM iot.data t1 WHERE t1.id = " +
		"(SELECT t2.id FROM iot.data t2 WHERE t2.username = t1.username ORDER BY t2.id DESC LIMIT 1)")

	if err != nil {
		panic(err.Error()) // proper error handling instead of panic in your app
	}

	for results.Next() {
		var username_n sql.NullString
		var dashboards_public_n, dashboards_private_n, devices_public_n,
			devices_private_n, iot_apps_n, etl_writes_n sql.NullInt64
		var username string
		var dashboards_public, dashboards_private, devices_public,
			devices_private, iot_apps, etl_writes int64

		// for each row, scan the result into our tag composite object
		err = results.Scan(&username_n, &dashboards_public_n,
			&dashboards_private_n, &devices_public_n, &devices_private_n, &iot_apps_n, &etl_writes_n)

		// deal with null values
		if username_n.Valid {
			username = username_n.String
		} else {
			username = ""
		}
		if dashboards_public_n.Valid {
			dashboards_public = dashboards_public_n.Int64
		} else {
			dashboards_public = 0
		}
		if dashboards_private_n.Valid {
			dashboards_private = dashboards_private_n.Int64
		} else {
			dashboards_private = 0
		}
		if devices_public_n.Valid {
			devices_public = devices_public_n.Int64
		} else {
			devices_public = 0
		}
		if devices_private_n.Valid {
			devices_private = devices_private_n.Int64
		} else {
			devices_private = 0
		}
		if iot_apps_n.Valid {
			iot_apps = iot_apps_n.Int64
		} else {
			iot_apps = 0
		}
		if etl_writes_n.Valid {
			etl_writes = etl_writes_n.Int64
		} else {
			etl_writes = 0
		}

		// user's level 0
		level = 0
		// user's level 1
		if dashboards_public > 0 || dashboards_private > 0 {
			level = 1
		}
		// user's level 2
		if ok := stringInSlice(username, brokerDashboardUsers); ok {
			level = 2
		}
		// user's level 3
		if ok := (devices_public > 0 || devices_private > 0) &&
			stringInSlice(username, brokerDashboardUsers); ok {
			level = 3
		}
		// user's level 4
		if (dashboards_public > 0 || dashboards_private > 0) &&
			(devices_public > 0 || devices_private > 0) &&
			iot_apps > 0 {
			level = 4
		}
		// user's level 5
		if (dashboards_public > 0 || dashboards_private > 0) &&
			(devices_public > 0 || devices_private > 0) &&
			iot_apps > 0 &&
			etl_writes > 0 {
			level = 5
		}
		// user's level 6
		if ok := users_roles[username] != "" && users_roles[username] == "ToolAdmin"; ok {
			level = 6
		}
		// user's level 7
		if ok := users_roles[username] != "" && users_roles[username] == "RootAdmin"; ok {
			level = 7
		}
		// add user's level into the map
		users_levels[username] = level
	}
	return users_levels
}

// insert user's levels into MySQL
func insertUsersLevels(users_roles map[string]string, users_organizations map[string][]string, users_levels map[string]int) {
	// setup MySQL connections
	db, err := sql.Open("mysql", "user:passw@tcp(localhost:3306)/iot")

	// if there is an error opening the connection, handle it
	if err != nil {
		panic(err.Error())
	}

	// defer the close until the main function has finished executing
	defer db.Close()

	stmt, err := db.Prepare("INSERT IGNORE INTO roles_levels (username, role, organizations, level)" +
		"VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE role = ?, organizations = ?, level = ?, " +
		"date = CURRENT_TIMESTAMP")
	if err != nil {
		panic(err.Error())
	}

	level := 0
	organizations := []byte{}

	for username, role := range users_roles {
		if users_levels[username] >= 0 {
			level = users_levels[username]
		} else {
			level = 0
		}

		// set level for users not present in users_levels (with any activity)
		if users_roles[username] == "ToolAdmin" {
			level = 6
		}
		if users_roles[username] == "RootAdmin" {
			level = 7
		}
		if len(users_organizations[username]) > 0 {
			organizations, _ = json.Marshal(users_organizations[username])
		} else {
			organizations, _ = json.Marshal([]string{})
		}

		_, err := stmt.Exec(username, role, string(organizations), level, role, organizations, level)

		if err != nil {
			panic(err.Error())
		}
	}
}

func main() {
	// get users => roles map from LDAP
	users_roles := getUsersRoles()

	// get users who created dashboards through device IoT broker
	brokerDashboardUsers := getBrokerDashboardUsers()

	// get users => level map
	users_levels := getUsersLevels(users_roles, brokerDashboardUsers)

	// get users => [organizations] map
	users_organizations := getUsersOrganizations()

	// insert user's levels into MySQL
	insertUsersLevels(users_roles, users_organizations, users_levels)
}
