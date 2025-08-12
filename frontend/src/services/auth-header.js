export default function authHeader() {
  const userStr = localStorage.getItem('user');
  console.log("Auth Header - User from localStorage:", userStr);
  
  if (!userStr) {
    console.log("Auth Header - No user found in localStorage");
    return {};
  }
  
  try {
    const user = JSON.parse(userStr);
    console.log("Auth Header - Parsed user:", user);
    
    if (user && user.token) {
      const authHeader = { 
        Authorization: 'Bearer ' + user.token,
        'Content-Type': 'application/json'
      };
      console.log("Auth Header - Sending header:", authHeader);
      return authHeader;
    } else {
      console.log("Auth Header - No token found in user object:", user);
      return {};
    }
  } catch (error) {
    console.error("Auth Header - Error parsing user from localStorage:", error);
    return {};
  }
} 