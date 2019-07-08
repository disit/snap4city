<?php

/**
 * Navigator: a geographic calculation library for PHP
 * @link http://navigator.simonholywell.com
 * @license http://www.opensource.org/licenses/bsd-license.php BSD 2-Clause License 
 * @copyright 2012, Simon Holywell
 * @author Simon Holywell <treffynnon@php.net>
 */

namespace Treffynnon\Navigator;

use Treffynnon\Navigator as N;

/**
 * A representation of a latitude and longitude coordinate 
 */
class LatLong {

    /**
     * Latitude coordinate store
     * @var Coordinate 
     */
    protected $latitude = null;

    /**
     * Longitude coordinate store
     * @var Coordinate 
     */
    protected $longitude = null;

    /**
     * Setup a new LatLng instance by passing in the latitude and longitude
     * @param Coordinate $latitude
     * @param Coordinate $longitude 
     */
    public function __construct(Coordinate $latitude, Coordinate $longitude) {
        $this->latitude = $latitude;
        $this->longitude = $longitude;
        $this->primeCoordinateParsers();
    }

    /**
     * Prime the coordinate parser with any additional information
     * necessary 
     */
    protected function primeCoordinateParsers() {
        $this->latitude->getParser()->setDirection(N::LAT);
        $this->longitude->getParser()->setDirection(N::LONG);
    }

    /**
     * Get the latitude coordinate object
     * @return Coordinate 
     */
    public function getLatitude() {
        return $this->latitude;
    }

    /**
     * Get the longitude coordinate object
     * @return Coordinate 
     */
    public function getLongitude() {
        return $this->longitude;
    }

    /**
     * Get string representation
     * @return string
     */
    public function __toString() {
        return $this->getLatitude() . ',' . $this->getLongitude();
    }

    /**
     * Create a LatLong object based on a comma separated coordinates string (latitude, longitude)
     *
     * @param  string $string
     * @return LatLong
     */
    public static function createFromString($string) {
        list($latitude, $longitude) = explode(',', $string);
        return new static(new Coordinate($latitude), new Coordinate($longitude));
    }
}