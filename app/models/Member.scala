package models

import anorm.{NotAssigned, Pk}

/**
 * Created by Javi on 5/16/14.
 */


class Member (id: Pk[Long] = NotAssigned, person: Person, role: Role){

}

class Person (id: Pk[Long] = NotAssigned, firstName: String, lastName: String){

}

class Role (id: Pk[Long] = NotAssigned, name: String){

}
