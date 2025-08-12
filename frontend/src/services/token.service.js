/**
 * Service for managing JWT tokens and user data in local storage
 */
class TokenService {
  getLocalRefreshToken() {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      console.log("Getting refresh token:", user?.refreshToken ? "Token exists" : "No token found");
      return user?.refreshToken;
    } catch (e) {
      console.error("Error retrieving refresh token:", e);
      return null;
    }
  }

  getLocalAccessToken() {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      console.log("Getting access token:", user?.token ? `Token exists: ${user.token.substring(0, 15)}...` : "No token found");
      return user?.token;
    } catch (e) {
      console.error("Error retrieving access token:", e);
      return null;
    }
  }

  updateLocalAccessToken(token) {
    try {
      let user = JSON.parse(localStorage.getItem('user'));
      user.token = token;
      localStorage.setItem('user', JSON.stringify(user));
      console.log("Access token updated:", token.substring(0, 15) + "...");
    } catch (e) {
      console.error("Error updating access token:", e);
    }
  }

  getUser() {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      console.log("Getting user from localStorage:", user ? "User found" : "No user found");
      if (user) {
        console.log("User details:", {
          username: user.username,
          hasToken: !!user.token,
          roles: user.roles
        });
      }
      return user;
    } catch (e) {
      console.error("Error retrieving user from localStorage:", e);
      return null;
    }
  }

  setUser(user) {
    try {
      localStorage.setItem('user', JSON.stringify(user));
      console.log("User saved to localStorage:", {
        username: user.username,
        hasToken: !!user.token,
        roles: user.roles
      });
    } catch (e) {
      console.error("Error saving user to localStorage:", e);
    }
  }

  removeUser() {
    try {
      localStorage.removeItem('user');
      console.log("User removed from localStorage");
    } catch (e) {
      console.error("Error removing user from localStorage:", e);
    }
  }
}

export default new TokenService(); 