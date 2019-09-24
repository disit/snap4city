<?php

/* Smart Cloud Engine Web Interface
  Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */

//get banned users
// TODO fare la query sulla tabella users where label is not null
function getBannedUsers($prefix) {
    return $prefix . "user != 'd2fd74a68f195145d283cf293cde94d1e5114b81b10b6551a996a2b17f532805' AND " .
            $prefix . "user != '911c62dc12d8ad8aebda0dd7c1675be0d32f0edb90e29b6e9e71c77b45626b96' AND " .
            $prefix . "user != '42767b11352d69d6408ffcc8216a4438017cbbcc44ba5d07abd6ff38d859d519' AND " .
            $prefix . "user != '07bbdafed8706ec39030bd22cc75bdc191b5e0d87b5d511d7cc44e1df70645ee' AND " .
            $prefix . "user != 'b8fdcc28133b4391037a8609dfe1bb656c40cfb408c76918632bd9d698322918' AND " .
            $prefix . "user != 'fcc35ae7e786b9dbf9839a096b0c98bb9565ccbbb3306425cda76417cab27020' AND " .
            $prefix . "user != '36767b11352d69d6408ffcc8216a4438017cbbcc44ba5d07abd6ff38d859d519' AND " .
            $prefix . "user != '42767b11352d69d6408ffcc8216a4438017cbbcc44ba5d07abd6ff38d8000000' AND " .
            $prefix . "user != '816ed0f50f318132d21500e9f8a6a4c51fbcd3313a787d6ed833a6e3774814be' AND  " .
            $prefix . "user !='61fe2e7407dcad9adf93d72d013befc173e19c93e136dc49c2a35016c359f719' AND " .
            $prefix . "user !='0000000011111111222222223333333344444444555555556666666677777777'";
}

?>