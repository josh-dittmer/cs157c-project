package org.group2;

import java.util.List;
import java.util.Scanner;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.types.Node;

public class Main {
    private static final String URI = "bolt://localhost:7687";
    private static final String DB_USER = "neo4j";
    private static final String DB_PASSWORD = "password123";

    private static Driver driver;
    private static final Scanner scanner = new Scanner(System.in);
    private static String loggedInUsername = null;

    public static void main(String[] args) {
        driver = GraphDatabase.driver(URI, AuthTokens.basic(DB_USER, DB_PASSWORD));

        try {
            testConnection();
            runMenu();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Make sure the Neo4j Docker container is running.");
        } finally {
            driver.close();
            scanner.close();
        }
    }

    private static void testConnection() {
        try (Session session = driver.session()) {
            String result = session.executeRead(tx -> tx.run("RETURN 'Connected to Neo4j successfully' AS message")
                    .single()
                    .get("message")
                    .asString());
            System.out.println(result);
        }
    }

    private static void runMenu() {
        while (true) {
            System.out.println("\n========== Social Network Console ==========");
            if (loggedInUsername == null) {
                System.out.println("Not logged in");
            } else {
                System.out.println("Logged in as: " + loggedInUsername);
            }

            System.out.println("1. Register New User");
            System.out.println("2. Login");
            System.out.println("3. View My Profile");
            System.out.println("4. Edit My Profile");
            System.out.println("5. Follow Another User");
            System.out.println("6. Unfollow a User");
            System.out.println("7. View Friends/Connections");
            System.out.println("8. Mutual Connections");
            System.out.println("9. Friend Recommendations");
            System.out.println("10. Search Users");
            System.out.println("11. Explore Popular Users");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registerUser();
                    break;
                case "2":
                    loginUser();
                    break;
                case "3":
                    viewProfile();
                    break;
                case "4":
                    editProfile();
                    break;
                case "5":
                    followUser();
                    break;
                case "6":
                    unfollowUser();
                    break;
                case "7":
                    viewConnections();
                    break;
                case "8":
                    mutualConnections();
                    break;
                case "9":
                    friendRecommendations();
                    break;
                case "10":
                    searchUsers();
                    break;
                case "11":
                    explorePopularUsers();
                    break;
                case "0":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    // UC-1: User Registration
    private static void registerUser() {
        System.out.println("\n--- UC-1: User Registration ---");

        System.out.print("Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Bio: ");
        String bio = scanner.nextLine().trim();

        if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            System.out.println("Registration failed. Name, email, username, and password are required.");
            return;
        }

        try (Session session = driver.session()) {
            Boolean usernameExists = session.executeRead(tx -> {
                Result result = tx.run(
                        "MATCH (u:User {username: $username}) RETURN count(u) AS count",
                        parameters("username", username));
                return result.single().get("count").asLong() > 0;
            });

            if (usernameExists) {
                System.out.println("Registration failed. Username already exists.");
                return;
            }

            Integer newId = session.executeWrite(tx -> {
                Result result = tx.run(
                        """
                                OPTIONAL MATCH (u:User)
                                WITH coalesce(max(u.id), 0) + 1 AS newId
                                CREATE (newUser:User {
                                    id: newId,
                                    name: $name,
                                    email: $email,
                                    username: $username,
                                    password: $password,
                                    bio: $bio
                                })
                                RETURN newUser.id AS id
                                """,
                        parameters(
                                "name", name,
                                "email", email,
                                "username", username,
                                "password", password,
                                "bio", bio));
                return result.single().get("id").asInt();
            });

            System.out.println("Registration successful.");
            System.out.println("New user ID: " + newId);
            System.out.println("Username: " + username);

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
        }
    }

    // UC-2: User Login
    private static void loginUser() {
        System.out.println("\n--- UC-2: User Login ---");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try (Session session = driver.session()) {
            Boolean validLogin = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (u:User {username: $username, password: $password})
                                RETURN count(u) AS count
                                """,
                        parameters("username", username, "password", password));
                return result.single().get("count").asLong() == 1;
            });

            if (validLogin) {
                loggedInUsername = username;
                System.out.println("Login successful. Welcome, " + username + ".");
            } else {
                System.out.println("Login failed. Invalid username or password.");
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        }
    }

    // UC-3: View Profile
    private static void viewProfile() {
        System.out.println("\n--- UC-3: View Profile ---");

        if (!requireLogin()) {
            return;
        }

        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (u:User {username: $username})
                                RETURN u.id AS id,
                                       u.name AS name,
                                       u.email AS email,
                                       u.username AS username,
                                       coalesce(u.bio, '') AS bio
                                """,
                        parameters("username", loggedInUsername));

                if (!result.hasNext()) {
                    System.out.println("Profile not found.");
                    return null;
                }

                org.neo4j.driver.Record record = result.single();

                System.out.println("User ID: " + record.get("id").asInt());
                System.out.println("Name: " + record.get("name").asString());
                System.out.println("Email: " + record.get("email").asString());
                System.out.println("Username: " + record.get("username").asString());
                System.out.println("Bio: " + record.get("bio").asString());

                return null;
            });

        } catch (Exception e) {
            System.out.println("View profile error: " + e.getMessage());
        }
    }

    // UC-4: Edit Profile
    private static void editProfile() {
        System.out.println("\n--- UC-4: Edit Profile ---");

        if (!requireLogin()) {
            return;
        }

        System.out.print("New name: ");
        String name = scanner.nextLine().trim();

        System.out.print("New email: ");
        String email = scanner.nextLine().trim();

        System.out.print("New bio: ");
        String bio = scanner.nextLine().trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.out.println("Update failed. Name and email cannot be empty.");
            return;
        }

        try (Session session = driver.session()) {
            Boolean updated = session.executeWrite(tx -> {
                Result result = tx.run(
                        """
                                MATCH (u:User {username: $username})
                                SET u.name = $name,
                                    u.email = $email,
                                    u.bio = $bio
                                RETURN count(u) AS count
                                """,
                        parameters(
                                "username", loggedInUsername,
                                "name", name,
                                "email", email,
                                "bio", bio));
                return result.single().get("count").asLong() == 1;
            });

            if (updated) {
                System.out.println("Profile updated successfully.");
            } else {
                System.out.println("Profile update failed.");
            }

        } catch (Exception e) {
            System.out.println("Edit profile error: " + e.getMessage());
        }
    }

    // UC-5: Follow Another User
    private static void followUser() {
        System.out.println("\n--- UC-5: Follow Another User ---");

        if (!requireLogin()) {
            return;
        }

        System.out.print("Enter username to follow: ");
        String targetUsername = scanner.nextLine().trim();

        if (targetUsername.equals(loggedInUsername)) {
            System.out.println("You cannot follow yourself.");
            return;
        }

        try (Session session = driver.session()) {
            String followedUsername = session.executeWrite(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $myUsername})
                                MATCH (target:User {username: $targetUsername})
                                WHERE me <> target
                                MERGE (me)-[:FOLLOWS]->(target)
                                RETURN target.username AS username
                                """,
                        parameters(
                                "myUsername", loggedInUsername,
                                "targetUsername", targetUsername));

                if (!result.hasNext()) {
                    return null;
                }

                return result.single().get("username").asString();
            });

            if (followedUsername == null) {
                System.out.println("Follow failed. Target user was not found.");
            } else {
                System.out.println("Now following: " + followedUsername);
            }

        } catch (Exception e) {
            System.out.println("Follow error: " + e.getMessage());
        }
    }

    // UC-6: Unfollow a User
    private static void unfollowUser() {
        System.out.println("\n--- UC-6: Unfollow a User ---");

        if (!requireLogin()) {
            return;
        }

        System.out.print("Enter username to unfollow: ");
        String targetUsername = scanner.nextLine().trim();

        try (Session session = driver.session()) {
            String unfollowedUsername = session.executeWrite(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $myUsername})-[r:FOLLOWS]->(target:User {username: $targetUsername})
                                WITH r, target
                                DELETE r
                                RETURN target.username AS username
                                """,
                        parameters(
                                "myUsername", loggedInUsername,
                                "targetUsername", targetUsername));

                if (!result.hasNext()) {
                    return null;
                }

                return result.single().get("username").asString();
            });

            if (unfollowedUsername == null) {
                System.out.println("Unfollow failed. You are not following that user, or the user does not exist.");
            } else {
                System.out.println("Unfollowed: " + unfollowedUsername);
            }

        } catch (Exception e) {
            System.out.println("Unfollow error: " + e.getMessage());
        }
    }

    // UC-7: View Friends/Connections
    private static void viewConnections() {
        System.out.println("\n--- UC-7: View Friends/Connections ---");

        if (!requireLogin())
            return;

        try (Session session = driver.session()) {
            // People I follow
            List<String> following = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $username})-[:FOLLOWS]->(target:User)
                                RETURN target.name AS name, target.username AS username
                                ORDER BY target.name
                                """,
                        parameters("username", loggedInUsername));
                return result.list(r -> r.get("name").asString() + " (@" + r.get("username").asString() + ")");
            });

            // People following me
            List<String> followers = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $username})<-[:FOLLOWS]-(follower:User)
                                RETURN follower.name AS name, follower.username AS username
                                ORDER BY follower.name
                                """,
                        parameters("username", loggedInUsername));
                return result.list(r -> r.get("name").asString() + " (@" + r.get("username").asString() + ")");
            });

            System.out.println("\nFollowing (" + following.size() + "):");
            if (following.isEmpty()) {
                System.out.println("\tNone");
            } else {
                following.forEach(u -> System.out.println("\t" + u));
            }

            System.out.println("\nFollowers (" + followers.size() + "):");
            if (followers.isEmpty()) {
                System.out.println("\tNone");
            } else {
                followers.forEach(u -> System.out.println("\t" + u));
            }

        } catch (Exception e) {
            System.out.println("View connections error: " + e.getMessage());
        }
    }

    // UC-8: Mutual Connections
    private static void mutualConnections() {
        System.out.println("\n--- UC-8: Mutual Connections ---");

        if (!requireLogin())
            return;

        System.out.print("Enter username to find mutual connections with: ");
        String targetUsername = scanner.nextLine().trim();

        try (Session session = driver.session()) {
            List<String> mutuals = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $myUsername})-[:FOLLOWS]->(mutual:User)<-[:FOLLOWS]-(other:User {username: $targetUsername})
                                RETURN mutual.name AS name, mutual.username AS username
                                ORDER BY mutual.name
                                """,
                        parameters("myUsername", loggedInUsername, "targetUsername", targetUsername));
                return result.list(r -> r.get("name").asString() + " (@" + r.get("username").asString() + ")");
            });

            if (mutuals.isEmpty()) {
                System.out.println("No mutual connections found with @" + targetUsername + ".");
            } else {
                System.out.println("Mutual connections with @" + targetUsername + " (" + mutuals.size() + "):");
                mutuals.forEach(u -> System.out.println("\t" + u));
            }

        } catch (Exception e) {
            System.out.println("Mutual connections error: " + e.getMessage());
        }
    }

    // UC-9: Friend Recommendations
    private static void friendRecommendations() {
        System.out.println("\n--- UC-9: Friend Recommendations ---");

        if (!requireLogin())
            return;

        try (Session session = driver.session()) {
            List<String> recommendations = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                MATCH (me:User {username: $username})-[:FOLLOWS]->(a:User)-[:FOLLOWS]->(rec:User)
                                WHERE rec <> me
                                AND NOT (me)-[:FOLLOWS]->(rec)
                                RETURN rec.name AS name, rec.username AS username, count(a) AS commonCount
                                ORDER BY commonCount DESC
                                LIMIT 10
                                """,
                        parameters("username", loggedInUsername));
                return result.list(r -> r.get("name").asString() + " (@" + r.get("username").asString() + ") — "
                        + r.get("commonCount").asInt() + " mutual");
            });

            if (recommendations.isEmpty()) {
                System.out.println("No recommendations yet. Try following more users first.");
            } else {
                System.out.println("Recommended users to follow:");
                recommendations.forEach(u -> System.out.println("\t" + u));
            }

        } catch (Exception e) {
            System.out.println("Friend recommendations error: " + e.getMessage());
        }
    }

    // UC-10: Search Users
    private static void searchUsers() {
        /*
         * The following index has been built on the database:
         * 
         * CREATE FULLTEXT INDEX fulltext_index
         * FOR (u:User)
         * ON EACH [u.name, u.username];
         * 
         */

        System.out.println("\n--- UC-10: Search Users ---");

        System.out.print("Enter the user's name or username: ");
        String targetQuery = scanner.nextLine().trim();

        try (Session session = driver.session()) {
            List<User> users = session.executeRead(tx -> {
                Result result = tx.run(
                        """
                                CALL db.index.fulltext.queryNodes("fulltext_index", $targetQuery)
                                YIELD node, score
                                RETURN node, score ORDER BY score DESC LIMIT 5;
                                """,
                        parameters(
                                "targetQuery", "*" + targetQuery + "~*"));

                return result.list(record -> {
                    Node node = record.get("node").asNode();

                    User user = new User();
                    user.setName(node.get("name").asString(""));
                    user.setEmail(node.get("email").asString(""));
                    user.setUsername(node.get("username").asString(""));
                    user.setPassword(node.get("password").asString(""));

                    return user;
                });
            });

            System.out.println("Search results:");
            for (User user : users) {
                System.out.println("\t" + user.getName() + " (@" + user.getUsername() + ")");
            }
        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    // UC-11: Explore Popular Users
    private static void explorePopularUsers() {
        System.out.println("\n--- UC-11: Explore Popular Users ---");

        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                Result result = tx.run(
                        """
                                MATCH (u:User)
                                RETURN u, COUNT{(u)<-[:FOLLOWS]-()} AS numFollowers ORDER BY numFollowers DESC LIMIT 5
                                """);

                System.out.println("Most popular users:");

                while (result.hasNext()) {
                    Record record = result.next();
                    Node node = record.get("u").asNode();

                    String name = node.get("name").asString("");
                    int numFollowers = record.get("numFollowers").asInt();

                    System.out.println("\t" + name + ": " + numFollowers + " followers");
                }
            });
        } catch (Exception e) {
            System.out.println("Explore popular error: " + e.getMessage());
        }
    }

    private static boolean requireLogin() {
        if (loggedInUsername == null) {
            System.out.println("You must log in first.");
            return false;
        }
        return true;
    }
}